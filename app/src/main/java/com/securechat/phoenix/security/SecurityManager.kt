package com.securechat.phoenix.security

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central security manager for the app.
 * Handles FLAG_SECURE, clipboard protection, auto-lock timing.
 */
@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    private var lastInteractionTime = System.currentTimeMillis()
    private val clipboardHandler = Handler(Looper.getMainLooper())
    private var clipboardClearRunnable: Runnable? = null

    companion object {
        const val AUTO_LOCK_30S = 30_000L
        const val AUTO_LOCK_1M = 60_000L
        const val AUTO_LOCK_5M = 300_000L
        const val AUTO_LOCK_15M = 900_000L
        const val AUTO_LOCK_30M = 1_800_000L
        const val CLIPBOARD_CLEAR_DELAY = 30_000L // 30 seconds
    }

    var autoLockTimeout: Long = AUTO_LOCK_5M // Default 5 minutes

    /**
     * Apply FLAG_SECURE to prevent screenshots and screen recording.
     * Also hides content in recent apps.
     */
    fun applyFlagSecure(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Record user interaction (call on each touch/input event).
     */
    fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Check if auto-lock should trigger (called on app resume).
     * Returns true if user should be sent back to passcode screen.
     */
    fun shouldAutoLock(): Boolean {
        val elapsed = System.currentTimeMillis() - lastInteractionTime
        return elapsed > autoLockTimeout
    }

    /**
     * Schedule clipboard clear after 30 seconds.
     * Called when user copies a message.
     */
    fun scheduleClipboardClear() {
        clipboardClearRunnable?.let { clipboardHandler.removeCallbacks(it) }

        clipboardClearRunnable = Runnable {
            clearClipboard()
        }
        clipboardHandler.postDelayed(clipboardClearRunnable!!, CLIPBOARD_CLEAR_DELAY)
    }

    /**
     * Clear clipboard immediately (called on app exit).
     */
    fun clearClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                @Suppress("DEPRECATION")
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {}
    }

    /**
     * Cancel any pending clipboard clear.
     */
    fun cancelClipboardClear() {
        clipboardClearRunnable?.let { clipboardHandler.removeCallbacks(it) }
    }
}
