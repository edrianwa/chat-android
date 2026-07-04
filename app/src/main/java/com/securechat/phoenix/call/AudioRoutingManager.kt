package com.securechat.phoenix.call

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio routing for voice calls:
 * - Earpiece (default for calls)
 * - Speaker
 * - Bluetooth
 * - Wired headset
 *
 * Also handles proximity sensor wake lock.
 */
@Singleton
class AudioRoutingManager @Inject constructor(
    private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var proximityWakeLock: PowerManager.WakeLock? = null

    enum class Route {
        EARPIECE,
        SPEAKER,
        BLUETOOTH,
        WIRED_HEADSET
    }

    var currentRoute: Route = Route.EARPIECE
        private set

    /**
     * Start call audio mode.
     */
    fun startCallAudio() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        currentRoute = Route.EARPIECE
        acquireProximityWakeLock()
    }

    /**
     * Switch to speaker.
     */
    fun setSpeaker(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
        currentRoute = if (enabled) Route.SPEAKER else Route.EARPIECE

        if (enabled) {
            releaseProximityWakeLock()
        } else {
            acquireProximityWakeLock()
        }
    }

    /**
     * Toggle speaker on/off.
     */
    fun toggleSpeaker(): Boolean {
        val newState = !audioManager.isSpeakerphoneOn
        setSpeaker(newState)
        return newState
    }

    /**
     * Check if a wired headset is connected.
     */
    fun isWiredHeadsetConnected(): Boolean {
        return audioManager.isWiredHeadsetOn
    }

    /**
     * Stop call audio mode and release resources.
     */
    fun stopCallAudio() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        currentRoute = Route.EARPIECE
        releaseProximityWakeLock()
    }

    private fun acquireProximityWakeLock() {
        if (proximityWakeLock == null) {
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "phoenix:call_proximity"
            )
        }
        if (proximityWakeLock?.isHeld == false) {
            proximityWakeLock?.acquire(60 * 60 * 1000L) // max 1 hour
        }
    }

    private fun releaseProximityWakeLock() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock?.release()
        }
    }
}
