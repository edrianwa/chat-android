package com.securechat.phoenix.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Manages game sound effects using SoundPool for low-latency playback.
 * Generates simple synthesized sounds programmatically (no asset files needed).
 */
class GameSoundManager(context: Context) {

    private val soundPool: SoundPool
    private var flapSoundId: Int = 0
    private var scoreSoundId: Int = 0
    private var crashSoundId: Int = 0
    private var isLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isLoaded = true
        }

        // Load sound effects from raw resources
        try {
            flapSoundId = soundPool.load(context, getResourceId(context, "sfx_flap"), 1)
            scoreSoundId = soundPool.load(context, getResourceId(context, "sfx_score"), 1)
            crashSoundId = soundPool.load(context, getResourceId(context, "sfx_crash"), 1)
        } catch (_: Exception) {
            // Sounds are optional — game works without them
        }
    }

    private fun getResourceId(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    fun playFlap() {
        if (isLoaded && flapSoundId != 0) {
            soundPool.play(flapSoundId, 0.5f, 0.5f, 1, 0, 1.2f)
        }
    }

    fun playScore() {
        if (isLoaded && scoreSoundId != 0) {
            soundPool.play(scoreSoundId, 0.6f, 0.6f, 1, 0, 1.0f)
        }
    }

    fun playCrash() {
        if (isLoaded && crashSoundId != 0) {
            soundPool.play(crashSoundId, 0.8f, 0.8f, 1, 0, 0.8f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
