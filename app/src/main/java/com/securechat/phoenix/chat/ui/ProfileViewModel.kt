package com.securechat.phoenix.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.auth.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val userApi: UserApi
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Load profile from local token data first (instant), then fetch fresh from server.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            // 1. Populate immediately from locally stored token data
            val localId = tokenManager.getUserId()
            val localUniqueId = tokenManager.getUniqueId()
            val localName = tokenManager.displayName.first()

            _profile.value = UserProfile(
                id = localId ?: "",
                uniqueId = localUniqueId ?: "",
                displayName = localName ?: "Phoenix User",
                about = ""
            )

            // 2. Fetch fresh from server
            try {
                val response = userApi.getMyProfile()
                if (response.isSuccessful && response.body() != null) {
                    val serverProfile = response.body()!!
                    _profile.value = UserProfile(
                        id = serverProfile.id,
                        uniqueId = serverProfile.uniqueId,
                        displayName = serverProfile.displayName,
                        about = serverProfile.about ?: "",
                        avatarUrl = serverProfile.avatarUrl
                    )
                }
            } catch (_: Exception) {
                // Offline — keep local data
            }
        }
    }

    fun updateDisplayName(name: String) {
        if (name.length !in 2..64) return

        val current = _profile.value
        _profile.value = current.copy(displayName = name)

        viewModelScope.launch {
            try {
                val body = mapOf("displayName" to name)
                userApi.updateProfile(body)
            } catch (_: Exception) {
                // Revert on failure
                _profile.value = current
            }
        }
    }

    fun updateAbout(about: String) {
        if (about.length > 256) return

        val current = _profile.value
        _profile.value = current.copy(about = about)

        viewModelScope.launch {
            try {
                val body = mapOf("about" to about)
                userApi.updateProfile(body)
            } catch (_: Exception) {
                _profile.value = current
            }
        }
    }
}
