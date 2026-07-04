package com.securechat.phoenix.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStateMachineTest {

    @Test
    fun `IDLE can transition to OUTGOING_RINGING`() {
        assertTrue(CallStateMachine.canTransition(CallState.IDLE, CallState.OUTGOING_RINGING))
    }

    @Test
    fun `IDLE can transition to INCOMING_RINGING`() {
        assertTrue(CallStateMachine.canTransition(CallState.IDLE, CallState.INCOMING_RINGING))
    }

    @Test
    fun `IDLE cannot transition to CONNECTED`() {
        assertFalse(CallStateMachine.canTransition(CallState.IDLE, CallState.CONNECTED))
    }

    @Test
    fun `OUTGOING_RINGING can transition to CONNECTING`() {
        assertTrue(CallStateMachine.canTransition(CallState.OUTGOING_RINGING, CallState.CONNECTING))
    }

    @Test
    fun `OUTGOING_RINGING can transition to MISSED`() {
        assertTrue(CallStateMachine.canTransition(CallState.OUTGOING_RINGING, CallState.MISSED))
    }

    @Test
    fun `OUTGOING_RINGING can transition to REJECTED`() {
        assertTrue(CallStateMachine.canTransition(CallState.OUTGOING_RINGING, CallState.REJECTED))
    }

    @Test
    fun `INCOMING_RINGING can transition to CONNECTING`() {
        assertTrue(CallStateMachine.canTransition(CallState.INCOMING_RINGING, CallState.CONNECTING))
    }

    @Test
    fun `INCOMING_RINGING can transition to REJECTED`() {
        assertTrue(CallStateMachine.canTransition(CallState.INCOMING_RINGING, CallState.REJECTED))
    }

    @Test
    fun `CONNECTING can transition to CONNECTED`() {
        assertTrue(CallStateMachine.canTransition(CallState.CONNECTING, CallState.CONNECTED))
    }

    @Test
    fun `CONNECTING can transition to FAILED`() {
        assertTrue(CallStateMachine.canTransition(CallState.CONNECTING, CallState.FAILED))
    }

    @Test
    fun `CONNECTED can transition to ENDED`() {
        assertTrue(CallStateMachine.canTransition(CallState.CONNECTED, CallState.ENDED))
    }

    @Test
    fun `CONNECTED can transition to FAILED`() {
        assertTrue(CallStateMachine.canTransition(CallState.CONNECTED, CallState.FAILED))
    }

    @Test
    fun `CONNECTED cannot transition to IDLE directly`() {
        assertFalse(CallStateMachine.canTransition(CallState.CONNECTED, CallState.IDLE))
    }

    @Test
    fun `ENDED can transition to IDLE`() {
        assertTrue(CallStateMachine.canTransition(CallState.ENDED, CallState.IDLE))
    }

    @Test
    fun `MISSED can transition to IDLE`() {
        assertTrue(CallStateMachine.canTransition(CallState.MISSED, CallState.IDLE))
    }

    @Test
    fun `FAILED can transition to IDLE`() {
        assertTrue(CallStateMachine.canTransition(CallState.FAILED, CallState.IDLE))
    }

    @Test
    fun `transition returns target when valid`() {
        val result = CallStateMachine.transition(CallState.IDLE, CallState.OUTGOING_RINGING)
        assertEquals(CallState.OUTGOING_RINGING, result)
    }

    @Test
    fun `transition returns current when invalid`() {
        val result = CallStateMachine.transition(CallState.IDLE, CallState.CONNECTED)
        assertEquals(CallState.IDLE, result)
    }

    @Test
    fun `CallSession duration is zero before connect`() {
        val session = CallSession(state = CallState.OUTGOING_RINGING)
        assertEquals(0, session.durationSeconds)
    }

    @Test
    fun `CallSession isActive true when CONNECTED`() {
        val session = CallSession(state = CallState.CONNECTED)
        assertTrue(session.isActive)
    }

    @Test
    fun `CallSession isActive true when CONNECTING`() {
        val session = CallSession(state = CallState.CONNECTING)
        assertTrue(session.isActive)
    }

    @Test
    fun `CallSession isActive false when IDLE`() {
        val session = CallSession(state = CallState.IDLE)
        assertFalse(session.isActive)
    }

    @Test
    fun `CallSession isRinging true for OUTGOING_RINGING`() {
        val session = CallSession(state = CallState.OUTGOING_RINGING)
        assertTrue(session.isRinging)
    }

    @Test
    fun `CallSession isRinging true for INCOMING_RINGING`() {
        val session = CallSession(state = CallState.INCOMING_RINGING)
        assertTrue(session.isRinging)
    }

    @Test
    fun `CallSession isRinging false for CONNECTED`() {
        val session = CallSession(state = CallState.CONNECTED)
        assertFalse(session.isRinging)
    }

    @Test
    fun `full outgoing call lifecycle`() {
        var state = CallState.IDLE
        state = CallStateMachine.transition(state, CallState.OUTGOING_RINGING)
        assertEquals(CallState.OUTGOING_RINGING, state)
        state = CallStateMachine.transition(state, CallState.CONNECTING)
        assertEquals(CallState.CONNECTING, state)
        state = CallStateMachine.transition(state, CallState.CONNECTED)
        assertEquals(CallState.CONNECTED, state)
        state = CallStateMachine.transition(state, CallState.ENDED)
        assertEquals(CallState.ENDED, state)
        state = CallStateMachine.transition(state, CallState.IDLE)
        assertEquals(CallState.IDLE, state)
    }

    @Test
    fun `incoming call rejected lifecycle`() {
        var state = CallState.IDLE
        state = CallStateMachine.transition(state, CallState.INCOMING_RINGING)
        assertEquals(CallState.INCOMING_RINGING, state)
        state = CallStateMachine.transition(state, CallState.REJECTED)
        assertEquals(CallState.REJECTED, state)
        state = CallStateMachine.transition(state, CallState.IDLE)
        assertEquals(CallState.IDLE, state)
    }

    @Test
    fun `outgoing call missed lifecycle`() {
        var state = CallState.IDLE
        state = CallStateMachine.transition(state, CallState.OUTGOING_RINGING)
        state = CallStateMachine.transition(state, CallState.MISSED)
        assertEquals(CallState.MISSED, state)
    }
}
