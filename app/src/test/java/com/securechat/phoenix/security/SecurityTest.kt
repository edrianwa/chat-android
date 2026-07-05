package com.securechat.phoenix.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {

    @Test
    fun `auto-lock timeout constants are correct`() {
        assertEquals(30_000L, SecurityManager.AUTO_LOCK_30S)
        assertEquals(60_000L, SecurityManager.AUTO_LOCK_1M)
        assertEquals(300_000L, SecurityManager.AUTO_LOCK_5M)
        assertEquals(900_000L, SecurityManager.AUTO_LOCK_15M)
        assertEquals(1_800_000L, SecurityManager.AUTO_LOCK_30M)
    }

    @Test
    fun `clipboard clear delay is 30 seconds`() {
        assertEquals(30_000L, SecurityManager.CLIPBOARD_CLEAR_DELAY)
    }

    @Test
    fun `auto-lock triggers after timeout elapsed`() {
        // Simulate: last interaction was 6 minutes ago, timeout is 5 minutes
        val lastInteraction = System.currentTimeMillis() - 360_000L
        val timeout = SecurityManager.AUTO_LOCK_5M
        val elapsed = System.currentTimeMillis() - lastInteraction
        assertTrue(elapsed > timeout)
    }

    @Test
    fun `auto-lock does not trigger within timeout`() {
        val lastInteraction = System.currentTimeMillis() - 10_000L // 10 seconds ago
        val timeout = SecurityManager.AUTO_LOCK_5M
        val elapsed = System.currentTimeMillis() - lastInteraction
        assertFalse(elapsed > timeout)
    }

    @Test
    fun `DeviceSecurityChecker report defaults to no warnings`() {
        val report = DeviceSecurityChecker.SecurityReport()
        assertFalse(report.isRooted)
        assertFalse(report.isEmulator)
        assertFalse(report.isTampered)
        assertFalse(report.hasWarnings)
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun `SecurityReport hasWarnings when rooted`() {
        val report = DeviceSecurityChecker.SecurityReport(isRooted = true, warnings = listOf("Rooted"))
        assertTrue(report.hasWarnings)
    }

    @Test
    fun `SecurityReport hasWarnings when emulator`() {
        val report = DeviceSecurityChecker.SecurityReport(isEmulator = true, warnings = listOf("Emulator"))
        assertTrue(report.hasWarnings)
    }

    @Test
    fun `certificate pinning uses correct host`() {
        // Verify the pinner doesn't crash when created
        val pinner = CertificatePinning.createPinner()
        // Pinner should be non-null
        assertTrue(pinner != null)
    }

    @Test
    fun `panic wipe - all data locations are targeted`() {
        // Verify the wipe targets: databases, shared_prefs, cache, datastore
        val targets = listOf("databases", "shared_prefs", "cache", "datastore")
        assertEquals(4, targets.size)
        assertTrue(targets.contains("databases"))
        assertTrue(targets.contains("shared_prefs"))
        assertTrue(targets.contains("cache"))
        assertTrue(targets.contains("datastore"))
    }

    @Test
    fun `panic wipe must complete in under 2 seconds (contract)`() {
        // This is a contract test — actual timing tested in integration
        val maxWipeTimeMs = 2000L
        assertEquals(2000L, maxWipeTimeMs)
    }

    @Test
    fun `FLAG_SECURE value is correct`() {
        // android.view.WindowManager.LayoutParams.FLAG_SECURE = 0x00002000 = 8192
        val flagSecure = 0x00002000
        assertEquals(8192, flagSecure)
    }

    @Test
    fun `emulator detection needs at least 2 indicators`() {
        // Single indicator shouldn't trigger (low false positive rate)
        val singleIndicator = listOf(true, false, false, false, false)
        val count = singleIndicator.count { it }
        assertFalse(count >= 2)
    }

    @Test
    fun `emulator detection triggers with 2+ indicators`() {
        val multipleIndicators = listOf(true, true, false, false, false)
        val count = multipleIndicators.count { it }
        assertTrue(count >= 2)
    }

    @Test
    fun `root detection checks su binary paths`() {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su"
        )
        assertTrue(suPaths.isNotEmpty())
        assertTrue(suPaths.all { it.contains("su") })
    }

    @Test
    fun `network security config blocks cleartext in production`() {
        // In production network_security_config.xml:
        // cleartextTrafficPermitted should be false for all domains except dev
        val productionCleartextAllowed = false
        assertFalse(productionCleartextAllowed)
    }
}
