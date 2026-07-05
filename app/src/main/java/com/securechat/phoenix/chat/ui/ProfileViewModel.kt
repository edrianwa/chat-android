package com.securechat.phoenix.chat.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.auth.UserApi
import com.securechat.phoenix.media.network.MediaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val userApi: UserApi,
    private val mediaApi: MediaApi
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val localId = tokenManager.getUserId()
            val localUniqueId = tokenManager.getUniqueId()
            val localName = tokenManager.displayName.first()

            _profile.value = UserProfile(
                id = localId ?: "",
                uniqueId = localUniqueId ?: "",
                displayName = localName ?: "Phoenix User",
                about = ""
            )

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
            } catch (_: Exception) {}
        }
    }

    /**
     * Upload avatar image from URI to server, then update profile.
     */
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                // Read image bytes from URI
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.readBytes()
                } ?: return@launch

                // Upload to media endpoint
                val requestBody = bytes.toRequestBody()
                val userId = tokenManager.getUserId() ?: return@launch
                val response = mediaApi.upload(
                    chatId = userId, // Use own user ID as chat for avatar storage
                    fileName = "avatar_${System.currentTimeMillis()}.jpg",
                    contentType = "image/jpeg",
                    body = requestBody
                )

                if (response.isSuccessful && response.body() != null) {
                    val mediaUrl = response.body()!!.url

                    // Update profile with new avatar URL
                    val updateResponse = userApi.updateProfile(mapOf("avatarUrl" to mediaUrl))
                    if (updateResponse.isSuccessful) {
                        _profile.value = _profile.value.copy(avatarUrl = mediaUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun updateDisplayName(name: String) {
        if (name.length !in 2..64) return
        val current = _profile.value
        _profile.value = current.copy(displayName = name)
        viewModelScope.launch {
            try {
                userApi.updateProfile(mapOf("displayName" to name))
            } catch (_: Exception) {
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
                userApi.updateProfile(mapOf("about" to about))
            } catch (_: Exception) {
                _profile.value = current
            }
        }
    }
}
