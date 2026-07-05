package com.securechat.phoenix.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Detects root, emulator, and tampering indicators.
 * Shows warning (doesn't block) if detected.
 */
object DeviceSecurityChecker {

    data class SecurityReport(
        val isRooted: Boolean = false,
        val isEmulator: Boolean = false,
        val isTampered: Boolean = false,
        val warnings: List<String> = emptyList()
    ) {
        val hasWarnings: Boolean get() = isRooted || isEmulator || isTampered
    }

    /**
     * Run all security checks and return a report.
     */
    fun check(context: Context): SecurityReport {
        val warnings = mutableListOf<String>()
        val rooted = checkRoot()
        val emulator = checkEmulator()
        val tampered = checkTampering(context)

        if (rooted) warnings.add("Device appears to be rooted")
        if (emulator) warnings.add("Running on an emulator")
        if (tampered) warnings.add("App signature mismatch detected")

        return SecurityReport(
            isRooted = rooted,
            isEmulator = emulator,
            isTampered = tampered,
            warnings = warnings
        )
    }

    /**
     * Check for common root indicators.
     */
    private fun checkRoot(): Boolean {
        // Check su binary in common paths
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su"
        )
        if (suPaths.any { File(it).exists() }) return true

        // Check for Magisk
        if (File("/sbin/.magisk").exists() || File("/cache/.disable_magisk").exists()) return true

        // Check build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        // Check for dangerous apps (simplified — just check su paths above)
        return false
    }

    /**
     * Check for emulator signatures.
     */
    private fun checkEmulator(): Boolean {
        val indicators = listOf(
            Build.FINGERPRINT.startsWith("generic"),
            Build.FINGERPRINT.startsWith("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK built for x86"),
            Build.MANUFACTURER.contains("Genymotion"),
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"),
            Build.PRODUCT == "sdk_gphone64_arm64",
            Build.PRODUCT == "sdk_gphone_x86",
            Build.HARDWARE.contains("goldfish"),
            Build.HARDWARE.contains("ranchu"),
        )
        return indicators.count { it } >= 2
    }

    /**
     * Verify app signature (detect repackaging).
     */
    private fun checkTampering(context: Context): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            // In production: compare against known release signing certificate hash
            // For now, always return false (no baseline to compare against in debug)
            false
        } catch (_: Exception) {
            false
        }
    }
}
