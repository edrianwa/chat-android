package com.securechat.phoenix.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.chat.data.ChatRepository
import com.securechat.phoenix.chat.data.MessageEntity
import com.securechat.phoenix.chat.data.SendResult
import com.securechat.phoenix.chat.network.ChatSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val conversations: List<MessageEntity> = emptyList(),
    val currentChatId: String? = null,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socketClient: ChatSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations().collect { conversations ->
                _uiState.value = _uiState.value.copy(conversations = conversations)
            }
        }
    }

    fun openChat(chatId: String) {
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

            val result = socketClient.sendMessage(recipientId, content)

            when (result) {
                is SendResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSending = false, error = null)
                }
                is SendResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun markAsRead(senderId: String, messageIds: List<String>) {
        socketClient.sendReadReceipt(senderId, messageIds)
    }
}
