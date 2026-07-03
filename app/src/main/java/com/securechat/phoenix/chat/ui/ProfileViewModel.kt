package com.securechat.phoenix.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.data.PasscodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val passcodeRepository: PasscodeRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            // In a full implementation this would come from the server/local DB
            // For now generate a consistent ID from the device
            val storedId = generateLocalUserId()
            _profile.value = UserProfile(
                id = storedId,
                uniqueId = storedId,
                displayName = "Phoenix User",
                about = "Hey there! I am using Phoenix"
            )
        }
    }

    fun updateDisplayName(name: String) {
        if (name.length in 2..64) {
            _profile.value = _profile.value.copy(displayName = name)
            // TODO: API call to update on server
        }
    }

    fun updateAbout(about: String) {
        if (about.length <= 256) {
            _profile.value = _profile.value.copy(about = about)
            // TODO: API call to update on server
        }
    }

    private fun generateLocalUserId(): String {
        // Generate a stable 8-digit ID from system properties
        val seed = (android.os.Build.FINGERPRINT + android.os.Build.DEVICE).hashCode()
        val id = (kotlin.math.abs(seed) % 90000000 + 10000000)
        return id.toString()
    }
}
