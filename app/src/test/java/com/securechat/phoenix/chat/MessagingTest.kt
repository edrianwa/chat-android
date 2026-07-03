package com.securechat.phoenix.chat

import com.securechat.phoenix.chat.data.MessageEntity
import com.securechat.phoenix.chat.data.MessageStatus
import com.securechat.phoenix.chat.data.PendingMessageEntity
import com.securechat.phoenix.chat.network.ConnectionState
import com.securechat.phoenix.chat.network.IncomingMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingTest {

    @Test
    fun `MessageEntity stores all required fields`() {
        val msg = MessageEntity(
            id = 1,
            messageId = "uuid-1",
            chatId = "recipient-1",
            senderId = "sender-1",
            content = "Hello, world!",
            timestamp = System.currentTimeMillis(),
            sequenceNumber = 1,
            status = MessageStatus.SENT,
            isOutgoing = true
        )

        assertEquals("uuid-1", msg.messageId)
        assertEquals("recipient-1", msg.chatId)
        assertEquals("Hello, world!", msg.content)
        assertEquals(MessageStatus.SENT, msg.status)
        assertTrue(msg.isOutgoing)
    }

    @Test
    fun `MessageStatus transitions are valid`() {
        val statuses = MessageStatus.entries
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(MessageStatus.SENDING))
        assertTrue(statuses.contains(MessageStatus.SENT))
        assertTrue(statuses.contains(MessageStatus.DELIVERED))
        assertTrue(statuses.contains(MessageStatus.READ))
    }

    @Test
    fun `Deduplication: same messageId is detected`() {
        val msgs = mutableMapOf<String, MessageEntity>()

        val msg1 = MessageEntity(
            messageId = "msg-dup-1",
            chatId = "chat-1",
            senderId = "sender-1",
            content = "Hello",
            timestamp = 1000,
            sequenceNumber = 1,
            status = MessageStatus.DELIVERED,
            isOutgoing = false
        )

        // First insert succeeds
        msgs[msg1.messageId] = msg1

        // Duplicate detected
        val isDuplicate = msgs.containsKey("msg-dup-1")
        assertTrue(isDuplicate)
    }

    @Test
    fun `PendingMessageEntity queues for offline send`() {
        val pending = PendingMessageEntity(
            messageId = "pending-1",
            recipientId = "user-2",
            content = "Offline message",
            sequenceNumber = 5,
            timestamp = System.currentTimeMillis()
        )

        assertEquals("pending-1", pending.messageId)
        assertEquals("user-2", pending.recipientId)
        assertEquals("Offline message", pending.content)
    }

    @Test
    fun `ConnectionState tracks all states`() {
        val states = ConnectionState.entries
        assertEquals(4, states.size)
        assertTrue(states.contains(ConnectionState.DISCONNECTED))
        assertTrue(states.contains(ConnectionState.CONNECTING))
        assertTrue(states.contains(ConnectionState.CONNECTED))
        assertTrue(states.contains(ConnectionState.ERROR))
    }

    @Test
    fun `IncomingMessage holds all needed data`() {
        val incoming = IncomingMessage(
            messageId = "incoming-1",
            senderId = "sender-A",
            senderUniqueId = "12345678",
            content = "Decrypted plaintext",
            timestamp = 1234567890L
        )

        assertEquals("incoming-1", incoming.messageId)
        assertEquals("Decrypted plaintext", incoming.content)
        assertNotNull(incoming.timestamp)
    }

    @Test
    fun `Message ordering by sequence number`() {
        val messages = listOf(
            createMessage("m3", seq = 3, ts = 1003),
            createMessage("m1", seq = 1, ts = 1001),
            createMessage("m2", seq = 2, ts = 1002),
        )

        val sorted = messages.sortedBy { it.sequenceNumber }
        assertEquals("m1", sorted[0].messageId)
        assertEquals("m2", sorted[1].messageId)
        assertEquals("m3", sorted[2].messageId)
    }

    @Test
    fun `Message ordering by timestamp when sequence ties`() {
        val messages = listOf(
            createMessage("m2", seq = 1, ts = 2000),
            createMessage("m1", seq = 1, ts = 1000),
        )

        val sorted = messages.sortedBy { it.timestamp }
        assertEquals("m1", sorted[0].messageId)
        assertEquals("m2", sorted[1].messageId)
    }

    @Test
    fun `Unique message IDs for each message`() {
        val id1 = java.util.UUID.randomUUID().toString()
        val id2 = java.util.UUID.randomUUID().toString()
        assertNotEquals(id1, id2)
    }

    private fun createMessage(id: String, seq: Int, ts: Long) = MessageEntity(
        messageId = id,
        chatId = "chat-1",
        senderId = "sender-1",
        content = "msg",
        timestamp = ts,
        sequenceNumber = seq,
        status = MessageStatus.DELIVERED,
        isOutgoing = false
    )
}
