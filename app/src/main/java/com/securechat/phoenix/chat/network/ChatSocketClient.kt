package com.securechat.phoenix.chat.network

import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.chat.data.ChatRepository
import com.securechat.phoenix.chat.data.MessageStatus
import com.securechat.phoenix.chat.data.SendResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles message sending/receiving.
 * Currently stores messages locally and marks as sent.
 * Socket.io real-time relay will be integrated with socket.io-client-java library.
 */
@Singleton
class ChatSocketClient @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 100)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages

    /**
     * Send a message. Stores locally and marks as sent.
     */
    suspend fun sendMessage(recipientId: String, plaintext: String): SendResult {
        val userId = tokenManager.getUserId() ?: "local-user"

        val result = chatRepository.sendMessage(recipientId, plaintext, userId)

        if (result is SendResult.Success) {
            // Mark as sent immediately (message stored locally)
            chatRepository.updateMessageStatus(result.messageId, MessageStatus.SENT)
        }

        return result
    }

    /**
     * Handle delivery receipt.
     */
    suspend fun handleDeliveryReceipt(messageId: String) {
        chatRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
    }

    /**
     * Handle read receipt.
     */
    suspend fun handleReadReceipt(messageIds: List<String>) {
        chatRepository.updateMessageStatusBatch(messageIds, MessageStatus.READ)
    }

    /**
     * Send read receipt for messages from a sender.
     */
    fun sendReadReceipt(senderId: String, messageIds: List<String>) {
        scope.launch {
            chatRepository.updateMessageStatusBatch(messageIds, MessageStatus.READ)
        }
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class IncomingMessage(
    val messageId: String,
    val senderId: String,
    val senderUniqueId: String,
    val content: String,
    val timestamp: Long
)
