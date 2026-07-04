package com.securechat.phoenix.call

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

/**
 * Manages Picture-in-Picture mode for video calls.
 * When user navigates away during a video call, shows PiP window.
 */
object PictureInPictureHelper {

    /**
     * Check if PiP is supported on this device.
     */
    fun isPipSupported(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /**
     * Enter PiP mode during an active video call.
     */
    fun enterPipMode(activity: Activity) {
        if (!isPipSupported(activity)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16)) // Portrait video
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    /**
     * Update PiP params (e.g., change aspect ratio for landscape).
     */
    fun updatePipParams(activity: Activity, isLandscape: Boolean) {
        if (!isPipSupported(activity)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ratio = if (isLandscape) Rational(16, 9) else Rational(9, 16)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .build()
            activity.setPictureInPictureParams(params)
        }
    }
}
