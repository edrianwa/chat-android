package com.securechat.phoenix.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.data.PasscodeRepository
import com.securechat.phoenix.navigation.ScreenRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val chatPasscode: String = "",
    val decoyPasscode: String = "",
    val isComplete: Boolean = false,
    val error: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
    private val screenRegistry: ScreenRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onChatPasscodeChanged(value: String) {
        // Only allow numeric input
        val filtered = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(chatPasscode = filtered, error = null) }
    }

    fun onDecoyPasscodeChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(decoyPasscode = filtered, error = null) }
    }

    fun onConfirmSetup() {
        val state = _uiState.value

        // Validate
        if (state.chatPasscode.length < 4) {
            _uiState.update { it.copy(error = "Chat passcode must be at least 4 digits") }
            return
        }
        if (state.decoyPasscode.length < 4) {
            _uiState.update { it.copy(error = "Decoy passcode must be at least 4 digits") }
            return
        }
        if (state.chatPasscode == state.decoyPasscode) {
            _uiState.update { it.copy(error = "Passcodes must be different") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            // Clear existing and save new mappings
            passcodeRepository.clearAll()
            passcodeRepository.savePasscodeMapping(state.chatPasscode, "chat")
            passcodeRepository.savePasscodeMapping(state.decoyPasscode, "decoy_game")

            _uiState.update { it.copy(isComplete = true, isProcessing = false) }
        }
    }
}
