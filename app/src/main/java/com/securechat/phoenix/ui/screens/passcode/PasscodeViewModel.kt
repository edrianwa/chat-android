package com.securechat.phoenix.ui.screens.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.navigation.PasscodeRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasscodeUiState(
    val enteredDigits: String = "",
    val maxLength: Int = 6,
    val isLoading: Boolean = true,
    val needsSetup: Boolean = false,
    val resolvedRoute: String? = null,
    val error: String? = null
)

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val passcodeRouter: PasscodeRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasscodeUiState())
    val uiState: StateFlow<PasscodeUiState> = _uiState.asStateFlow()

    init {
        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        viewModelScope.launch {
            val isSetup = passcodeRouter.isSetupComplete()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsSetup = !isSetup
                )
            }
        }
    }

    fun onDigitPressed(digit: Int) {
        val current = _uiState.value.enteredDigits
        if (current.length >= _uiState.value.maxLength) return

        val newValue = current + digit.toString()
        _uiState.update { it.copy(enteredDigits = newValue, error = null) }

        // Auto-submit when max length reached
        if (newValue.length == _uiState.value.maxLength) {
            resolvePasscode(newValue)
        }
    }

    fun onDeletePressed() {
        val current = _uiState.value.enteredDigits
        if (current.isNotEmpty()) {
            _uiState.update {
                it.copy(enteredDigits = current.dropLast(1), error = null)
            }
        }
    }

    private fun resolvePasscode(passcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val route = passcodeRouter.resolveRoute(passcode)
            if (route != null) {
                _uiState.update { it.copy(resolvedRoute = route, isLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        enteredDigits = "",
                        isLoading = false,
                        error = "Invalid passcode"
                    )
                }
            }
        }
    }
}
