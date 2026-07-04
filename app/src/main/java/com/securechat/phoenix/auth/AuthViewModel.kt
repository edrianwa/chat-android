package com.securechat.phoenix.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val isReady: Boolean = false,
    val error: String? = null
)

/**
 * Handles silent auto-authentication using device ID.
 * No login/register UI needed — the passcode screen is the only gate.
 * Calls POST /auth/device which auto-registers or logs in the device.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        autoAuthenticate()
    }

    private fun autoAuthenticate() {
        viewModelScope.launch {
            // Already have a token? Skip
            val hasToken = tokenManager.isLoggedIn.first()
            if (hasToken) {
                _uiState.value = AuthUiState(isLoading = false, isReady = true)
                return@launch
            }

            // Call /auth/device — auto-registers or logs in
            val deviceId = getDeviceId()
            val displayName = Build.MODEL.take(32).ifEmpty { "Phoenix User" }

            try {
                val response = authApi.deviceAuth(DeviceAuthRequest(deviceId, displayName))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tokenManager.saveTokens(body.accessToken, body.refreshToken)
                    tokenManager.saveUser(body.user)
                    _uiState.value = AuthUiState(isLoading = false, isReady = true)
                } else {
                    // Server error — allow offline access
                    _uiState.value = AuthUiState(isLoading = false, isReady = true, error = "Offline mode")
                }
            } catch (e: Exception) {
                // Network error — allow offline access
                _uiState.value = AuthUiState(isLoading = false, isReady = true, error = null)
            }
        }
    }

    private fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val hash = androidId.hashCode()
        return (kotlin.math.abs(hash) % 90000000 + 10000000).toString()
    }
}
