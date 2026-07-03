package com.securechat.phoenix.chat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Message status representing delivery state.
 */
enum class MessageStatus {
    SENDING,    // Queued locally, not yet sent
    SENT,       // Sent to server (✓)
    DELIVERED,  // Delivered to recipient (✓✓)
    READ        // Read by recipient (blue ✓✓)
}

/**
 * Room entity for local message storage.
 * Stores decrypted plaintext locally (encrypted by SQLCipher).
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chat_id", "timestamp"]),
        Index(value = ["message_id"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "chat_id") val chatId: String, // recipientId for 1-on-1
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "sequence_number") val sequenceNumber: Int,
    @ColumnInfo(name = "status") val status: MessageStatus,
    @ColumnInfo(name = "is_outgoing") val isOutgoing: Boolean
)

/**
 * Pending outgoing message queued for sending when connection restores.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "recipient_id") val recipientId: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "sequence_number") val sequenceNumber: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
