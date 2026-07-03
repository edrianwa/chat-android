package com.securechat.phoenix.chat.network

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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.io client for real-time messaging.
 * Handles message sending, receiving, and receipt flow.
 */
@Singleton
class ChatSocketClient @Inject constructor(
    private val chatRepository: ChatRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 100)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages

    private var localUserId: String = ""

    /**
     * Connect to the chat server with JWT auth token.
     */
    fun connect(serverUrl: String, authToken: String, userId: String) {
        localUserId = userId
        _connectionState.value = ConnectionState.CONNECTING
        // In production: use socket.io-client-java library
        // For now this defines the contract and event flow
        _connectionState.value = ConnectionState.CONNECTED
        scope.launch { flushPendingMessages() }
    }

    /**
     * Disconnect from the chat server.
     */
    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Send a message through the socket.
     */
    suspend fun sendMessage(recipientId: String, plaintext: String): SendResult {
        val result = chatRepository.sendMessage(recipientId, plaintext, localUserId)

        if (result is SendResult.Success) {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                // Emit message:send event via Socket.io
                emitSendMessage(result)
                chatRepository.updateMessageStatus(result.messageId, MessageStatus.SENT)
            } else {
                // Queue for later
                chatRepository.queuePendingMessage(recipientId, plaintext)
            }
        }

        return result
    }

    /**
     * Process an incoming message from Socket.io.
     * Called when message:receive event is received.
     */
    suspend fun handleIncomingMessage(
        messageId: String,
        senderId: String,
        senderUniqueId: String,
        ciphertext: String,
        messageType: Int,
        sequenceNumber: Int,
        timestamp: Long
    ) {
        val message = chatRepository.receiveMessage(
            messageId = messageId,
            senderId = senderId,
            ciphertext = ciphertext,
            messageType = messageType,
            sequenceNumber = sequenceNumber,
            timestamp = timestamp,
            localUserId = localUserId
        )

        if (message != null) {
            // Emit delivery receipt
            emitDeliveryReceipt(messageId, senderId)

            // Notify UI
            _incomingMessages.emit(
                IncomingMessage(
                    messageId = messageId,
                    senderId = senderId,
                    senderUniqueId = senderUniqueId,
                    content = message.content,
                    timestamp = timestamp
                )
            )
        }
    }

    /**
     * Handle delivery receipt from server.
     */
    suspend fun handleDeliveryReceipt(messageId: String) {
        chatRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
    }

    /**
     * Handle read receipt from server.
     */
    suspend fun handleReadReceipt(messageIds: List<String>) {
        chatRepository.updateMessageStatusBatch(messageIds, MessageStatus.READ)
    }

    /**
     * Send read receipt for all messages from a sender.
     */
    fun sendReadReceipt(senderId: String, messageIds: List<String>) {
        scope.launch {
            chatRepository.updateMessageStatusBatch(messageIds, MessageStatus.READ)
            emitReadReceipt(senderId, messageIds)
        }
    }

    /**
     * Flush pending messages when connection restores.
     */
    private suspend fun flushPendingMessages() {
        val pending = chatRepository.getPendingMessages()
        for (msg in pending) {
            val result = chatRepository.sendMessage(msg.recipientId, msg.content, localUserId)
            if (result is SendResult.Success) {
                emitSendMessage(result)
                chatRepository.updateMessageStatus(result.messageId, MessageStatus.SENT)
                chatRepository.removePending(msg.messageId)
            }
        }
    }

    // --- Socket.io event emitters (contracts) ---

    private fun emitSendMessage(result: SendResult.Success) {
        // In production: socket.emit("message:send", payload, ack)
        // Payload: { messageId, recipientId, ciphertext, sequenceNumber }
    }

    private fun emitDeliveryReceipt(messageId: String, senderId: String) {
        // socket.emit("message:delivered", { messageId, senderId })
    }

    private fun emitReadReceipt(senderId: String, messageIds: List<String>) {
        // socket.emit("message:read", { messageIds, senderId })
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
