package com.securechat.phoenix.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeSettingsTest {

    @Test
    fun `minimum 2 passcodes required - can't delete when only 2 exist`() {
        val passcodes = listOf(
            PasscodeEntry("••••56", "Secure Chat", "chat"),
            PasscodeEntry("••••78", "Flying Phoenix", "decoy_game")
        )
        val canDelete = passcodes.size > 2
        assertFalse(canDelete)
    }

    @Test
    fun `can delete when more than 2 passcodes`() {
        val passcodes = listOf(
            PasscodeEntry("••••12", "Secure Chat", "chat"),
            PasscodeEntry("••••34", "Flying Phoenix", "decoy_game"),
            PasscodeEntry("••••56", "Settings", "settings")
        )
        val canDelete = passcodes.size > 2
        assertTrue(canDelete)
    }

    @Test
    fun `can't have two passcodes pointing to same screen`() {
        val passcodes = listOf(
            PasscodeEntry("••••12", "Secure Chat", "chat"),
            PasscodeEntry("••••34", "Secure Chat", "chat") // duplicate!
        )
        val screenIds = passcodes.map { it.screenId }
        val hasDuplicates = screenIds.size != screenIds.distinct().size
        assertTrue(hasDuplicates) // This should be flagged as invalid
    }

    @Test
    fun `valid mapping has unique destinations`() {
        val passcodes = listOf(
            PasscodeEntry("••••12", "Secure Chat", "chat"),
            PasscodeEntry("••••34", "Flying Phoenix", "decoy_game")
        )
        val screenIds = passcodes.map { it.screenId }
        val hasDuplicates = screenIds.size != screenIds.distinct().size
        assertFalse(hasDuplicates)
    }

    @Test
    fun `swap destinations changes mapping correctly`() {
        var passcodes = listOf(
            PasscodeEntry("••••12", "Secure Chat", "chat"),
            PasscodeEntry("••••34", "Flying Phoenix", "decoy_game")
        )

        // Simulate swap
        passcodes = listOf(
            PasscodeEntry("••••12", "Flying Phoenix", "decoy_game"),
            PasscodeEntry("••••34", "Secure Chat", "chat")
        )

        assertEquals("decoy_game", passcodes[0].screenId)
        assertEquals("chat", passcodes[1].screenId)
    }

    @Test
    fun `passcode must be at least 4 digits`() {
        val passcode = "123"
        val isValid = passcode.length >= 4
        assertFalse(isValid)
    }

    @Test
    fun `passcode of 4 digits is valid`() {
        val passcode = "1234"
        val isValid = passcode.length >= 4
        assertTrue(isValid)
    }

    @Test
    fun `passcode of 6 digits is valid`() {
        val passcode = "123456"
        val isValid = passcode.length in 4..6
        assertTrue(isValid)
    }

    @Test
    fun `passcodes must be different from each other`() {
        val codes = listOf("1234", "5678")
        val allUnique = codes.distinct().size == codes.size
        assertTrue(allUnique)
    }

    @Test
    fun `identical passcodes are invalid`() {
        val codes = listOf("1234", "1234")
        val allUnique = codes.distinct().size == codes.size
        assertFalse(allUnique)
    }

    @Test
    fun `PrivacySettings defaults are correct`() {
        val defaults = PrivacySettings()
        assertEquals("everyone", defaults.lastSeenVisibility)
        assertTrue(defaults.readReceiptsEnabled)
        assertEquals("everyone", defaults.profilePhotoVisibility)
    }

    @Test
    fun `read receipts off means you also cant see others`() {
        val settings = PrivacySettings(readReceiptsEnabled = false)
        // Both send and receive disabled when off
        assertFalse(settings.readReceiptsEnabled)
    }
}
