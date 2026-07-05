package com.securechat.phoenix.chat.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records voice messages using MediaRecorder.
 * Saves as M4A (AAC) format for efficient compression.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0L

    val isRecording: Boolean get() = recorder != null
    val durationMs: Long get() = if (isRecording) System.currentTimeMillis() - startTime else 0L

    /**
     * Start recording a voice message.
     * Returns the output file path.
     */
    fun startRecording(): File? {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            null
        }
    }

    /**
     * Stop recording and return the recorded file.
     */
    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val file = outputFile
            outputFile = null
            file
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            null
        }
    }

    /**
     * Cancel recording and delete the file.
     */
    fun cancelRecording() {
        cleanup()
        outputFile?.delete()
        outputFile = null
    }

    private fun cleanup() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        recorder = null
    }
}
