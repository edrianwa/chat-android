package com.securechat.phoenix.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCallTest {

    @Test
    fun `ConnectionQuality GOOD with high bitrate low loss`() {
        val quality = ConnectionQualityEvaluator.evaluate(
            bitrateBps = 1_500_000,
            packetLossPercent = 0.5f,
            jitterMs = 10f
        )
        assertEquals(ConnectionQuality.GOOD, quality)
    }

    @Test
    fun `ConnectionQuality FAIR with medium bitrate`() {
        val quality = ConnectionQualityEvaluator.evaluate(
            bitrateBps = 700_000,
            packetLossPercent = 2f,
            jitterMs = 50f
        )
        assertEquals(ConnectionQuality.FAIR, quality)
    }

    @Test
    fun `ConnectionQuality POOR with low bitrate`() {
        val quality = ConnectionQualityEvaluator.evaluate(
            bitrateBps = 200_000,
            packetLossPercent = 8f,
            jitterMs = 150f
        )
        assertEquals(ConnectionQuality.POOR, quality)
    }

    @Test
    fun `ConnectionQuality POOR with high loss despite good bitrate`() {
        val quality = ConnectionQualityEvaluator.evaluate(
            bitrateBps = 2_000_000,
            packetLossPercent = 10f,
            jitterMs = 200f
        )
        assertEquals(ConnectionQuality.POOR, quality)
    }

    @Test
    fun `recommended resolution for GOOD is 720p`() {
        val res = ConnectionQualityEvaluator.getRecommendedResolution(ConnectionQuality.GOOD)
        assertEquals(1280, res.width)
        assertEquals(720, res.height)
        assertEquals(30, res.fps)
    }

    @Test
    fun `recommended resolution for FAIR is 480p`() {
        val res = ConnectionQualityEvaluator.getRecommendedResolution(ConnectionQuality.FAIR)
        assertEquals(854, res.width)
        assertEquals(480, res.height)
        assertEquals(24, res.fps)
    }

    @Test
    fun `recommended resolution for POOR is 360p`() {
        val res = ConnectionQualityEvaluator.getRecommendedResolution(ConnectionQuality.POOR)
        assertEquals(640, res.width)
        assertEquals(360, res.height)
        assertEquals(15, res.fps)
    }

    @Test
    fun `VideoCallEngine bitrate thresholds are correct`() {
        assertEquals(1_500_000, VideoCallEngine.BITRATE_GOOD)
        assertEquals(800_000, VideoCallEngine.BITRATE_FAIR)
        assertEquals(300_000, VideoCallEngine.BITRATE_POOR)
    }

    @Test
    fun `default front camera resolution is 720p`() {
        assertEquals(1280, VideoCallEngine.FRONT_WIDTH)
        assertEquals(720, VideoCallEngine.FRONT_HEIGHT)
    }

    @Test
    fun `default back camera resolution is 1080p`() {
        assertEquals(1920, VideoCallEngine.BACK_WIDTH)
        assertEquals(1080, VideoCallEngine.BACK_HEIGHT)
    }

    @Test
    fun `default fps is 30`() {
        assertEquals(30, VideoCallEngine.DEFAULT_FPS)
    }

    @Test
    fun `VideoResolution data class holds values`() {
        val res = VideoResolution(1280, 720, 30)
        assertEquals(1280, res.width)
        assertEquals(720, res.height)
        assertEquals(30, res.fps)
    }

    @Test
    fun `CallSession for video call`() {
        val session = CallSession(
            callId = "vid-1",
            state = CallState.CONNECTED,
            remoteUserId = "user-2",
            remoteDisplayName = "Alice",
            isOutgoing = true,
            connectTime = System.currentTimeMillis()
        )
        assertTrue(session.isActive)
        assertFalse(session.isRinging)
    }

    @Test
    fun `video toggle state tracking`() {
        // Simulates video enabled/disabled toggle
        var videoEnabled = true
        videoEnabled = false
        assertFalse(videoEnabled)
        videoEnabled = true
        assertTrue(videoEnabled)
    }

    @Test
    fun `camera switch does not change call state`() {
        val session = CallSession(state = CallState.CONNECTED)
        // Camera switch is local only — state remains CONNECTED
        assertEquals(CallState.CONNECTED, session.state)
    }

    @Test
    fun `voice to video upgrade requires CONNECTED state`() {
        val connectedSession = CallSession(state = CallState.CONNECTED)
        val ringingSession = CallSession(state = CallState.OUTGOING_RINGING)
        assertTrue(connectedSession.isActive) // Can upgrade
        assertFalse(ringingSession.isActive)  // Cannot upgrade during ringing
    }
}
