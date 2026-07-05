package com.securechat.phoenix.chat.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Plays voice messages from local file paths.
 */
class VoicePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId

    /**
     * Play a voice message from file path.
     */
    fun play(messageId: String, filePath: String) {
        // Stop current if different
        if (currentFile != filePath) {
            stop()
        }

        if (_isPlaying.value && currentFile == filePath) {
            // Pause
            mediaPlayer?.pause()
            _isPlaying.value = false
            return
        }

        try {
            if (mediaPlayer == null || currentFile != filePath) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _currentPlayingId.value = null
                    }
                }
                currentFile = filePath
            }

            mediaPlayer?.start()
            _isPlaying.value = true
            _currentPlayingId.value = messageId
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    /**
     * Stop playback.
     */
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentFile = null
        _isPlaying.value = false
        _currentPlayingId.value = null
    }

    /**
     * Get duration of an audio file in seconds.
     */
    fun getDuration(filePath: String): Int {
        return try {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            val duration = mp.duration / 1000
            mp.release()
            duration
        } catch (e: Exception) {
            0
        }
    }

    fun release() {
        stop()
    }
}
