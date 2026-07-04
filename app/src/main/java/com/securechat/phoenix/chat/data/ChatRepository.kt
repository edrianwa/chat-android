package com.securechat.phoenix.chat.data

import android.util.Base64
import com.securechat.phoenix.crypto.session.SignalSessionManager
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing message operations — encryption, storage, deduplication.
 * Falls back to plaintext-over-TLS when Signal session can't be established
 * (e.g. recipient hasn't uploaded key bundle yet).
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
     * Tries Signal E2EE first, falls back to plaintext if keys unavailable.
     */
    suspend fun sendMessage(
        recipientId: String,
        plaintext: String,
        localUserId: String
    ): SendResult {
        val messageId = UUID.randomUUID().toString()
        val seq = ++sequenceNumber
        val timestamp = System.currentTimeMillis()

        // Try E2EE with Signal Protocol
        var ciphertext: String = Base64.encodeToString(plaintext.toByteArray(), Base64.NO_WRAP)
        var messageType = 0 // 0 = plaintext fallback

        try {
            if (sessionManager.hasSession(recipientId)) {
                val encrypted = sessionManager.encryptMessage(recipientId, plaintext)
                if (encrypted != null) {
                    ciphertext = encrypted.ciphertext
                    messageType = encrypted.type
                }
            } else {
                // Try to establish session
                val bundleResult = keyBundleRepository.fetchBundle(recipientId)
                bundleResult.onSuccess { bundle ->
                    val established = sessionManager.establishSession(recipientId, bundle)
                    if (established) {
                        val encrypted = sessionManager.encryptMessage(recipientId, plaintext)
                        if (encrypted != null) {
                            ciphertext = encrypted.ciphertext
                            messageType = encrypted.type
                        }
                    }
                }
                // If bundle fetch fails, we still send (plaintext fallback)
            }
        } catch (_: Exception) {
            // E2EE failed — send plaintext over TLS as fallback
        }

        // Store locally (always store plaintext locally)
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
            ciphertext = ciphertext,
            messageType = messageType,
            sequenceNumber = seq,
            timestamp = timestamp
        )
    }

    /**
     * Process a received message.
     * Tries to decrypt with Signal, falls back to Base64 decode for plaintext.
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
        // Deduplication
        val existing = messageDao.getMessageById(messageId)
        if (existing != null) return null

        // Decrypt
        var plaintext: String? = null

        if (messageType > 0) {
            // Signal Protocol encrypted
            plaintext = sessionManager.decryptMessage(senderId, ciphertext, messageType)
        }

        if (plaintext == null) {
            // Plaintext fallback (Base64 encoded)
            try {
                plaintext = String(Base64.decode(ciphertext, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                plaintext = ciphertext // Raw fallback
            }
        }

        val message = MessageEntity(
            messageId = messageId,
            chatId = senderId,
            senderId = senderId,
            content = plaintext ?: ciphertext,
            timestamp = timestamp,
            sequenceNumber = sequenceNumber,
            status = MessageStatus.DELIVERED,
            isOutgoing = false
        )
        messageDao.insertMessage(message)
        return message
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateStatus(messageId, status)
    }

    suspend fun updateMessageStatusBatch(messageIds: List<String>, status: MessageStatus) {
        messageDao.updateStatusBatch(messageIds, status)
    }

    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessages(chatId)
    }

    fun getConversations(): Flow<List<MessageEntity>> {
        return messageDao.getConversations()
    }

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

    suspend fun getPendingMessages(): List<PendingMessageEntity> {
        return messageDao.getPendingMessages()
    }

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
