package com.securechat.phoenix.call

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * WebRTC engine managing PeerConnection lifecycle for voice calls.
 * Handles offer/answer SDP creation and ICE candidate gathering.
 */
class WebRTCEngine(
    private val context: Context,
    private val listener: WebRTCListener
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    interface WebRTCListener {
        fun onLocalOffer(sdp: SessionDescription)
        fun onLocalAnswer(sdp: SessionDescription)
        fun onIceCandidate(candidate: IceCandidate)
        fun onConnected()
        fun onDisconnected()
        fun onFailed()
    }

    /**
     * Initialize WebRTC factory.
     */
    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    /**
     * Create a PeerConnection with ICE servers.
     */
    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { listener.onIceCandidate(it) }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> listener.onConnected()
                        PeerConnection.IceConnectionState.DISCONNECTED -> listener.onDisconnected()
                        PeerConnection.IceConnectionState.FAILED -> listener.onFailed()
                        else -> {}
                    }
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        )

        // Add local audio track
        addAudioTrack()
    }

    /**
     * Create and send an SDP offer (caller side).
     */
    fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    listener.onLocalOffer(it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { listener.onFailed() }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Set remote offer and create answer (callee side).
     */
    fun handleRemoteOffer(offerSdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), offerSdp)

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    listener.onLocalAnswer(it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { listener.onFailed() }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Set the remote answer SDP (caller side, after callee answers).
     */
    fun handleRemoteAnswer(answerSdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), answerSdp)
    }

    /**
     * Add a remote ICE candidate.
     */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    /**
     * Mute/unmute the local audio track.
     */
    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    /**
     * Clean up all WebRTC resources.
     */
    fun dispose() {
        localAudioTrack?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        peerConnection = null
        peerConnectionFactory = null
    }

    private fun addAudioTrack() {
        val factory = peerConnectionFactory ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }

        audioSource = factory.createAudioSource(constraints)
        localAudioTrack = factory.createAudioTrack("audio_local", audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack, listOf("stream_local"))
    }

    private class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
