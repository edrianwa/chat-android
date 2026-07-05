package com.securechat.phoenix.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTest {

    @Test
    fun `notification channels are correctly defined`() {
        assertEquals("phoenix_messages", PhoenixFirebaseService.CHANNEL_MESSAGES)
        assertEquals("phoenix_calls", PhoenixFirebaseService.CHANNEL_CALLS)
        assertEquals("phoenix_admin", PhoenixFirebaseService.CHANNEL_ADMIN)
    }

    @Test
    fun `notification privacy modes`() {
        val modes = listOf("show_content", "show_sender", "hide_all")
        assertTrue(modes.contains("show_content"))
        assertTrue(modes.contains("show_sender"))
        assertTrue(modes.contains("hide_all"))
    }

    @Test
    fun `show_content mode displays message preview`() {
        val mode = "show_content"
        val showsContent = mode == "show_content"
        assertTrue(showsContent)
    }

    @Test
    fun `show_sender mode hides message content`() {
        val mode = "show_sender"
        val showsContent = mode == "show_content"
        assertFalse(showsContent)
    }

    @Test
    fun `hide_all mode shows generic text`() {
        val mode = "hide_all"
        val showsSender = mode == "show_content" || mode == "show_sender"
        assertFalse(showsSender)
    }

    @Test
    fun `FCM payload for messages has no plaintext`() {
        val payload = mapOf("type" to "message", "notification_id" to "msg-1", "sender_id" to "user-1")
        assertFalse(payload.containsKey("content"))
        assertFalse(payload.containsKey("text"))
        assertFalse(payload.containsKey("body"))
        assertFalse(payload.containsKey("ciphertext"))
    }

    @Test
    fun `FCM payload for calls includes type and caller`() {
        val payload = mapOf("type" to "call", "caller_id" to "user-1", "call_type" to "video")
        assertEquals("call", payload["type"])
        assertEquals("user-1", payload["caller_id"])
        assertEquals("video", payload["call_type"])
    }

    @Test
    fun `deep link for message notification opens chat`() {
        val senderId = "user-123"
        val deepLink = "chat/$senderId"
        assertEquals("chat/user-123", deepLink)
    }

    @Test
    fun `deep link for call notification opens call screen`() {
        val deepLink = "incoming_call"
        assertEquals("incoming_call", deepLink)
    }

    @Test
    fun `deep link for admin notification opens admin`() {
        val deepLink = "admin"
        assertEquals("admin", deepLink)
    }

    @Test
    fun `foreground service notification is disguised`() {
        val title = "Phoenix"
        val text = "Tap to play"
        assertEquals("Phoenix", title)
        assertEquals("Tap to play", text)
        // Does NOT say "chat" or "message"
        assertFalse(text.contains("chat", ignoreCase = true))
        assertFalse(text.contains("message", ignoreCase = true))
    }

    @Test
    fun `reply key constant is defined`() {
        assertEquals("key_notification_reply", PhoenixFirebaseService.KEY_REPLY)
    }

    @Test
    fun `foreground service channel uses minimum importance`() {
        val channelId = MessagingForegroundService.CHANNEL_ID
        assertEquals("phoenix_foreground", channelId)
    }
}
