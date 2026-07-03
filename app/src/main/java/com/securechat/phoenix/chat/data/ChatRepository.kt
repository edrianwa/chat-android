package com.securechat.phoenix.chat.data

import com.securechat.phoenix.crypto.session.EncryptedMessage
import com.securechat.phoenix.crypto.session.SignalSessionManager
import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing message operations — encryption, storage, deduplication.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val sessionManager: SignalSessionManager,
    private val keyBundleRepository: KeyBundleRepository
) {
    private var sequenceNumber = 0

    /**
     * Send a message to a recipient.
     * Establishes a Signal session if none exists.
     * Returns the encrypted message ready for Socket.io transmission.
     */
    suspend fun sendMessage(
        recipientId: String,
        plaintext: String,
        localUserId: String
    ): SendResult {
        // Establish session if needed
        if (!sessionManager.hasSession(recipientId)) {
            val bundleResult = keyBundleRepository.fetchBundle(recipientId)
            val bundle = bundleResult.getOrElse {
                return SendResult.Error("Failed to fetch key bundle: ${it.message}")
            }
            val established = sessionManager.establishSession(recipientId, bundle)
            if (!established) {
                return SendResult.Error("Failed to establish secure session")
            }
        }

        // Encrypt
        val encrypted = sessionManager.encryptMessage(recipientId, plaintext)
            ?: return SendResult.Error("Encryption failed")

        // Generate message ID and sequence
        val messageId = UUID.randomUUID().toString()
        val seq = ++sequenceNumber
        val timestamp = System.currentTimeMillis()

        // Store locally
        messageDao.insertMessage(
            MessageEntity(
                messageId = messageId,
                chatId = recipientId,
                senderId = localUserId,
                content = plaintext,
                timestamp = timestamp,
                sequenceNumber = seq,
                status = MessageStatus.SENDING,
                isOutgoing = true
            )
        )

        return SendResult.Success(
            messageId = messageId,
            ciphertext = encrypted.ciphertext,
            messageType = encrypted.type,
            sequenceNumber = seq,
            timestamp = timestamp
        )
    }

    /**
     * Process a received encrypted message.
     * Decrypts and stores locally. Returns null if duplicate.
     */
    suspend fun receiveMessage(
        messageId: String,
        senderId: String,
        ciphertext: String,
        messageType: Int,
        sequenceNumber: Int,
        timestamp: Long,
        localUserId: String
    ): MessageEntity? {
        // Deduplication check
        val existing = messageDao.getMessageById(messageId)
        if (existing != null) return null

        // Decrypt
        val plaintext = sessionManager.decryptMessage(senderId, ciphertext, messageType)
            ?: return null

        // Store
        val message = MessageEntity(
            messageId = messageId,
            chatId = senderId,
            senderId = senderId,
            content = plaintext,
            timestamp = timestamp,
            sequenceNumber = sequenceNumber,
            status = MessageStatus.DELIVERED,
            isOutgoing = false
        )
        messageDao.insertMessage(message)
        return message
    }

    /**
     * Update message status from receipt.
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateStatus(messageId, status)
    }

    /**
     * Update multiple messages' status (batch read receipts).
     */
    suspend fun updateMessageStatusBatch(messageIds: List<String>, status: MessageStatus) {
        messageDao.updateStatusBatch(messageIds, status)
    }

    /**
     * Get messages for a conversation (reactive Flow).
     */
    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessages(chatId)
    }

    /**
     * Get all conversations with last message.
     */
    fun getConversations(): Flow<List<MessageEntity>> {
        return messageDao.getConversations()
    }

    /**
     * Queue a message for sending when offline.
     */
    suspend fun queuePendingMessage(recipientId: String, content: String) {
        val messageId = UUID.randomUUID().toString()
        messageDao.insertPending(
            PendingMessageEntity(
                messageId = messageId,
                recipientId = recipientId,
                content = content,
                sequenceNumber = ++sequenceNumber,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Get all queued pending messages.
     */
    suspend fun getPendingMessages(): List<PendingMessageEntity> {
        return messageDao.getPendingMessages()
    }

    /**
     * Remove a pending message after successful send.
     */
    suspend fun removePending(messageId: String) {
        messageDao.removePending(messageId)
    }
}

sealed class SendResult {
    data class Success(
        val messageId: String,
        val ciphertext: String,
        val messageType: Int,
        val sequenceNumber: Int,
        val timestamp: Long
    ) : SendResult()

    data class Error(val message: String) : SendResult()
}
