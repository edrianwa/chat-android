package com.securechat.phoenix.call

/**
 * Represents connection quality for a video call.
 * Determined from WebRTC stats (bitrate, packet loss, jitter).
 */
enum class ConnectionQuality {
    GOOD,   // > 1 Mbps, < 1% loss
    FAIR,   // 500 Kbps - 1 Mbps, 1-5% loss
    POOR    // < 500 Kbps, > 5% loss
}

/**
 * Evaluates connection quality from WebRTC stats.
 */
object ConnectionQualityEvaluator {

    fun evaluate(
        bitrateBps: Long,
        packetLossPercent: Float,
        jitterMs: Float
    ): ConnectionQuality {
        return when {
            bitrateBps >= 1_000_000 && packetLossPercent < 1f && jitterMs < 30f ->
                ConnectionQuality.GOOD
            bitrateBps >= 500_000 && packetLossPercent < 5f && jitterMs < 100f ->
                ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }

    /**
     * Get recommended video resolution for a given quality level.
     */
    fun getRecommendedResolution(quality: ConnectionQuality): VideoResolution {
        return when (quality) {
            ConnectionQuality.GOOD -> VideoResolution(1280, 720, 30)
            ConnectionQuality.FAIR -> VideoResolution(854, 480, 24)
            ConnectionQuality.POOR -> VideoResolution(640, 360, 15)
        }
    }
}

data class VideoResolution(
    val width: Int,
    val height: Int,
    val fps: Int
)
