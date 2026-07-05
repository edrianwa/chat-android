package com.securechat.phoenix.chat.network

import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.chat.data.ChatRepository
import com.securechat.phoenix.chat.data.MessageStatus
import com.securechat.phoenix.chat.data.SendResult
import io.socket.client.IO
import io.socket.client.Socket
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Socket.io client that connects to the server and sends/receives messages.
 */
@Singleton
class ChatSocketClient @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 100)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages

    companion object {
        private const val SERVER_URL = "http://10.0.2.2:3000"
    }

    /**
     * Connect to server with JWT token.
     */
    fun connect(authToken: String) {
        if (socket?.connected() == true) return

        try {
            val options = IO.Options().apply {
                auth = mapOf("token" to authToken)
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 30000
                timeout = 20000
            }

            socket = IO.socket(SERVER_URL, options)
            setupListeners()
            socket?.connect()
            _connectionState.value = ConnectionState.CONNECTING
        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Disconnect from server.
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Send a message via Socket.io.
     */
    suspend fun sendMessage(recipientId: String, plaintext: String): SendResult {
        val userId = tokenManager.getUserId() ?: "local-user"
        val result = chatRepository.sendMessage(recipientId, plaintext, userId)

        if (result is SendResult.Success) {
            val payload = JSONObject().apply {
                put("messageId", result.messageId)
                put("recipientId", recipientId)
                put("ciphertext", result.ciphertext)
                put("sequenceNumber", result.sequenceNumber)
            }

            if (socket?.connected() == true) {
                socket?.emit("message:send", payload)
                // Mark as sent since we emitted successfully
                chatRepository.updateMessageStatus(result.messageId, MessageStatus.SENT)
            } else {
                // Offline — message stored locally with SENDING status
                // Will be flushed when connection restores
                chatRepository.queuePendingMessage(recipientId, plaintext)
            }
        }

        return result
    }

    /**
     * Send read receipt to server.
     */
    fun sendReadReceipt(senderId: String, messageIds: List<String>) {
        scope.launch {
            chatRepository.updateMessageStatusBatch(messageIds, MessageStatus.READ)
        }

        if (socket?.connected() == true) {
            val payload = JSONObject().apply {
                put("senderId", senderId)
                put("messageIds", JSONArray(messageIds))
            }
            socket?.emit("message:read", payload)
        }
    }

    /**
     * Setup all Socket.io event listeners.
     */
    private fun setupListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            _connectionState.value = ConnectionState.CONNECTED
            scope.launch { flushPendingMessages() }
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            _connectionState.value = ConnectionState.ERROR
        }

        // Incoming message
        socket?.on("message:receive") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject ?: return@on
                scope.launch { handleIncomingMessage(data) }
            }
        }

        // Delivery receipt
        socket?.on("message:delivered") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject ?: return@on
                val messageId = data.optString("messageId")
                if (messageId.isNotEmpty()) {
                    scope.launch {
                        chatRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                    }
                }
            }
        }

        // Read receipt
        socket?.on("message:read") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject ?: return@on
                val idsArray = data.optJSONArray("messageIds")
                if (idsArray != null) {
                    val ids = (0 until idsArray.length()).map { idsArray.getString(it) }
                    scope.launch {
                        chatRepository.updateMessageStatusBatch(ids, MessageStatus.READ)
                    }
                }
            }
        }

        // Presence events
        socket?.on("presence:online") { args ->
            // TODO: update contact online status in local DB
        }

        socket?.on("presence:offline") { args ->
            // TODO: update contact offline status in local DB
        }
    }

    /**
     * Handle an incoming message from Socket.io.
     */
    private suspend fun handleIncomingMessage(data: JSONObject) {
        val messageId = data.optString("messageId")
        val senderId = data.optString("senderId")
        val senderUniqueId = data.optString("senderUniqueId", "")
        val ciphertext = data.optString("ciphertext")
        val sequenceNumber = data.optInt("sequenceNumber", 0)
        val timestamp = data.optLong("timestamp", System.currentTimeMillis())

        if (messageId.isEmpty() || senderId.isEmpty() || ciphertext.isEmpty()) return

        val userId = tokenManager.getUserId() ?: "local-user"
        val message = chatRepository.receiveMessage(
            messageId = messageId,
            senderId = senderId,
            ciphertext = ciphertext,
            messageType = 0, // plaintext fallback
            sequenceNumber = sequenceNumber,
            timestamp = timestamp,
            localUserId = userId
        )

        if (message != null) {
            // Emit delivery receipt back
            val receiptPayload = JSONObject().apply {
                put("messageId", messageId)
                put("senderId", senderId)
            }
            socket?.emit("message:delivered", receiptPayload)

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
     * Flush locally queued messages when connection restores.
     */
    private suspend fun flushPendingMessages() {
        val pending = chatRepository.getPendingMessages()
        val userId = tokenManager.getUserId() ?: "local-user"

        for (msg in pending) {
            val result = chatRepository.sendMessage(msg.recipientId, msg.content, userId)
            if (result is SendResult.Success && socket?.connected() == true) {
                val payload = JSONObject().apply {
                    put("messageId", result.messageId)
                    put("recipientId", msg.recipientId)
                    put("ciphertext", result.ciphertext)
                    put("sequenceNumber", result.sequenceNumber)
                }
                socket?.emit("message:send", payload)
                chatRepository.updateMessageStatus(result.messageId, MessageStatus.SENT)
                chatRepository.removePending(msg.messageId)
            }
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
