package com.securechat.phoenix.chat.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.auth.AuthApi
import com.securechat.phoenix.auth.DeviceAuthRequest
import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.chat.data.ChatRepository
import com.securechat.phoenix.chat.data.MessageEntity
import com.securechat.phoenix.chat.data.SendResult
import com.securechat.phoenix.chat.network.ChatSocketClient
import com.securechat.phoenix.chat.network.ConnectionState
import com.securechat.phoenix.crypto.KeyManager
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val conversations: List<MessageEntity> = emptyList(),
    val contactNames: Map<String, String> = emptyMap(),
    val currentChatId: String? = null,
    val isSending: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val socketClient: ChatSocketClient,
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val keyManager: KeyManager,
    private val keyBundleRepository: KeyBundleRepository,
    private val contactDao: com.securechat.phoenix.chat.data.ContactDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        ensureAuthenticatedAndConnect()
        loadConversations()
        loadContactNames()
        observeConnectionState()
    }

    /**
     * Authenticate silently, upload key bundle on first run, connect Socket.io.
     */
    private fun ensureAuthenticatedAndConnect() {
        viewModelScope.launch {
            var token = tokenManager.getAccessToken()

            if (token == null) {
                // Auto-register/login with device ID
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                val deviceId = (kotlin.math.abs(androidId.hashCode()) % 90000000 + 10000000).toString()
                val displayName = Build.MODEL.take(32).ifEmpty { "Phoenix User" }

                try {
                    val response = authApi.deviceAuth(DeviceAuthRequest(deviceId, displayName))
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        tokenManager.saveTokens(body.accessToken, body.refreshToken)
                        tokenManager.saveUser(body.user)
                        token = body.accessToken

                        // First-time: generate and upload Signal key bundle
                        uploadKeyBundleIfNeeded()
                    }
                } catch (_: Exception) {}
            }

            // Connect Socket.io
            if (token != null) {
                socketClient.connect(token)
                // Start foreground service to maintain connection in background
                com.securechat.phoenix.notifications.MessagingForegroundService.start(context)
                // Register FCM token with server for push notifications
                registerFcmToken(token)
            }
        }
    }

    /**
     * Generate Signal key bundle and upload to server.
     * Only runs once — skips if keys already exist locally.
     */
    private suspend fun uploadKeyBundleIfNeeded() {
        try {
            if (keyManager.hasKeys()) return // Already generated

            val bundle = keyManager.generateRegistrationKeys()
            keyBundleRepository.uploadBundle(bundle)
        } catch (e: Exception) {
            // Key generation requires native libsignal — non-fatal if unavailable
            android.util.Log.w("ChatViewModel", "Key bundle upload skipped: ${e.message}")
        }
    }

    /**
     * Register the device's FCM token with the server for push notifications.
     * Gets the token from Firebase and sends to POST /notifications/register-token.
     */
    private fun registerFcmToken(authToken: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { fcmToken ->
                    viewModelScope.launch {
                        com.securechat.phoenix.notifications.NotificationTokenManager
                            .registerCurrentToken(context, fcmToken, authToken)
                    }
                }
        } catch (e: Exception) {
            // Firebase not configured (no google-services.json) — non-fatal
            android.util.Log.w("ChatViewModel", "FCM token registration skipped: ${e.message}")
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            socketClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(isConnected = state == ConnectionState.CONNECTED)
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations().collect { conversations ->
                _uiState.value = _uiState.value.copy(conversations = conversations)
            }
        }
    }

    private fun loadContactNames() {
        viewModelScope.launch {
            contactDao.getAllContacts().collect { contacts ->
                val nameMap = contacts.associate { it.id to it.displayName }
                _uiState.value = _uiState.value.copy(contactNames = nameMap)
            }
        }
    }

    fun openChat(chatId: String) {
        if (_uiState.value.currentChatId == chatId) return
        _uiState.value = _uiState.value.copy(currentChatId = chatId)
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    fun sendMessage(recipientId: String, content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            // If image message, compress before sending
            val finalContent = if (content.startsWith("image:")) {
                val uriStr = content.removePrefix("image:")
                try {
                    val uri = android.net.Uri.parse(uriStr)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (originalBytes != null) {
                        // Compress: resize to max 1920px, convert to WebP
                        val compressed = com.securechat.phoenix.media.compression.ImageCompressor
                            .compressImageBytes(originalBytes)
                        if (compressed != null) {
                            // Save compressed to cache and use that URI
                            val compressedFile = java.io.File(
                                context.cacheDir,
                                "img_${System.currentTimeMillis()}.webp"
                            )
                            compressedFile.writeBytes(compressed.fullBytes)
                            "image:${android.net.Uri.fromFile(compressedFile)}"
                        } else content
                    } else content
                } catch (_: Exception) { content }
            } else content

            val result = socketClient.sendMessage(recipientId, finalContent)
            when (result) {
                is SendResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSending = false, error = null)
                }
                is SendResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSending = false, error = result.message)
                }
            }
        }
    }

    fun markAsRead(senderId: String, messageIds: List<String>) {
        socketClient.sendReadReceipt(senderId, messageIds)
    }
}
