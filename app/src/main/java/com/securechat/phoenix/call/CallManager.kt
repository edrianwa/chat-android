package com.securechat.phoenix.call

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central call manager that coordinates the WebRTC engine,
 * call state machine, and signaling events.
 */
@Singleton
class CallManager @Inject constructor(
    private val context: Context
) : WebRTCEngine.WebRTCListener {

    private var engine: WebRTCEngine? = null

    private val _callSession = MutableStateFlow(CallSession())
    val callSession: StateFlow<CallSession> = _callSession.asStateFlow()

    // Callback to send signaling messages (set by the socket client)
    var onSendOffer: ((String, SessionDescription) -> Unit)? = null
    var onSendAnswer: ((String, String, SessionDescription) -> Unit)? = null
    var onSendIceCandidate: ((String, IceCandidate) -> Unit)? = null
    var onSendEnd: ((String, String) -> Unit)? = null
    var onSendReject: ((String, String) -> Unit)? = null

    /**
     * Initiate an outgoing call.
     */
    fun initiateCall(calleeId: String, displayName: String, iceServers: List<PeerConnection.IceServer>) {
        if (_callSession.value.state != CallState.IDLE) return

        _callSession.value = CallSession(
            state = CallState.OUTGOING_RINGING,
            remoteUserId = calleeId,
            remoteDisplayName = displayName,
            isOutgoing = true,
            startTime = System.currentTimeMillis()
        )

        setupEngine(iceServers)
        engine?.createOffer()
    }

    /**
     * Handle an incoming call offer.
     */
    fun onIncomingCall(callId: String, callerId: String, displayName: String, offer: SessionDescription) {
        if (_callSession.value.state != CallState.IDLE) {
            // Already in a call — reject (busy)
            return
        }

        _callSession.value = CallSession(
            callId = callId,
            state = CallState.INCOMING_RINGING,
            remoteUserId = callerId,
            remoteDisplayName = displayName,
            isOutgoing = false,
            startTime = System.currentTimeMillis()
        )

        // Store the offer to process when user accepts
        pendingOffer = offer
    }

    private var pendingOffer: SessionDescription? = null

    /**
     * Accept an incoming call.
     */
    fun acceptCall(iceServers: List<PeerConnection.IceServer>) {
        if (_callSession.value.state != CallState.INCOMING_RINGING) return

        transitionState(CallState.CONNECTING)
        setupEngine(iceServers)

        pendingOffer?.let { offer ->
            engine?.handleRemoteOffer(offer)
            pendingOffer = null
        }
    }

    /**
     * Reject an incoming call.
     */
    fun rejectCall() {
        val session = _callSession.value
        if (session.state != CallState.INCOMING_RINGING) return

        onSendReject?.invoke(session.callId, session.remoteUserId)
        transitionState(CallState.REJECTED)
        cleanup()
    }

    /**
     * End an active call.
     */
    fun endCall() {
        val session = _callSession.value
        if (!session.isActive && !session.isRinging) return

        onSendEnd?.invoke(session.callId, session.remoteUserId)
        transitionState(CallState.ENDED)
        cleanup()
    }

    /**
     * Handle remote answer SDP (caller receives this after callee accepts).
     */
    fun onRemoteAnswer(answerSdp: SessionDescription) {
        transitionState(CallState.CONNECTING)
        engine?.handleRemoteAnswer(answerSdp)
    }

    /**
     * Handle remote ICE candidate.
     */
    fun onRemoteIceCandidate(candidate: IceCandidate) {
        engine?.addIceCandidate(candidate)
    }

    /**
     * Handle call rejected by remote.
     */
    fun onCallRejected() {
        transitionState(CallState.REJECTED)
        cleanup()
    }

    /**
     * Handle call ended by remote.
     */
    fun onCallEnded() {
        transitionState(CallState.ENDED)
        cleanup()
    }

    /**
     * Handle call timeout.
     */
    fun onCallTimeout() {
        transitionState(CallState.MISSED)
        cleanup()
    }

    /**
     * Toggle mute.
     */
    fun toggleMute() {
        val session = _callSession.value
        val newMuted = !session.isMuted
        _callSession.value = session.copy(isMuted = newMuted)
        engine?.setMuted(newMuted)
    }

    /**
     * Toggle speaker.
     */
    fun toggleSpeaker() {
        val session = _callSession.value
        _callSession.value = session.copy(isSpeakerOn = !session.isSpeakerOn)
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        cleanup()
        _callSession.value = CallSession()
    }

    // --- WebRTCListener callbacks ---

    override fun onLocalOffer(sdp: SessionDescription) {
        val session = _callSession.value
        onSendOffer?.invoke(session.remoteUserId, sdp)
    }

    override fun onLocalAnswer(sdp: SessionDescription) {
        val session = _callSession.value
        onSendAnswer?.invoke(session.callId, session.remoteUserId, sdp)
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        val session = _callSession.value
        onSendIceCandidate?.invoke(session.remoteUserId, candidate)
    }

    override fun onConnected() {
        _callSession.value = _callSession.value.copy(
            state = CallState.CONNECTED,
            connectTime = System.currentTimeMillis()
        )
    }

    override fun onDisconnected() {
        transitionState(CallState.ENDED)
        cleanup()
    }

    override fun onFailed() {
        transitionState(CallState.FAILED)
        cleanup()
    }

    // --- Private helpers ---

    private fun setupEngine(iceServers: List<PeerConnection.IceServer>) {
        engine = WebRTCEngine(context, this)
        engine?.initialize()
        engine?.createPeerConnection(iceServers)
    }

    private fun transitionState(target: CallState) {
        val current = _callSession.value.state
        val newState = CallStateMachine.transition(current, target)
        _callSession.value = _callSession.value.copy(state = newState)
    }

    private fun cleanup() {
        engine?.dispose()
        engine = null
        pendingOffer = null
    }
}
