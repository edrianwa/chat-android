package com.securechat.phoenix.call

/**
 * Call state machine representing all possible states of a voice call.
 */
enum class CallState {
    IDLE,               // No active call
    OUTGOING_RINGING,   // Caller waiting for callee to answer
    INCOMING_RINGING,   // Callee receiving incoming call
    CONNECTING,         // ICE/DTLS connecting after answer
    CONNECTED,          // Active call with audio
    ENDED,              // Call ended normally
    MISSED,             // Callee didn't answer (timeout)
    REJECTED,           // Callee rejected
    FAILED              // Connection failed
}

/**
 * Data class holding full call session state.
 */
data class CallSession(
    val callId: String = "",
    val state: CallState = CallState.IDLE,
    val remoteUserId: String = "",
    val remoteDisplayName: String = "",
    val isOutgoing: Boolean = false,
    val startTime: Long = 0L,
    val connectTime: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
) {
    val durationSeconds: Int
        get() = if (connectTime > 0) ((System.currentTimeMillis() - connectTime) / 1000).toInt() else 0

    val isActive: Boolean
        get() = state == CallState.CONNECTED || state == CallState.CONNECTING

    val isRinging: Boolean
        get() = state == CallState.OUTGOING_RINGING || state == CallState.INCOMING_RINGING
}

/**
 * Valid state transitions for the call state machine.
 */
object CallStateMachine {
    private val validTransitions = mapOf(
        CallState.IDLE to setOf(CallState.OUTGOING_RINGING, CallState.INCOMING_RINGING),
        CallState.OUTGOING_RINGING to setOf(CallState.CONNECTING, CallState.ENDED, CallState.MISSED, CallState.REJECTED, CallState.FAILED),
        CallState.INCOMING_RINGING to setOf(CallState.CONNECTING, CallState.ENDED, CallState.REJECTED, CallState.MISSED),
        CallState.CONNECTING to setOf(CallState.CONNECTED, CallState.FAILED, CallState.ENDED),
        CallState.CONNECTED to setOf(CallState.ENDED, CallState.FAILED),
        CallState.ENDED to setOf(CallState.IDLE),
        CallState.MISSED to setOf(CallState.IDLE),
        CallState.REJECTED to setOf(CallState.IDLE),
        CallState.FAILED to setOf(CallState.IDLE)
    )

    fun canTransition(from: CallState, to: CallState): Boolean {
        return validTransitions[from]?.contains(to) == true
    }

    fun transition(current: CallState, target: CallState): CallState {
        return if (canTransition(current, target)) target else current
    }
}
