package com.securechat.phoenix.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.auth.UserApi
import com.securechat.phoenix.chat.data.ContactDao
import com.securechat.phoenix.chat.data.ContactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<ContactItem> = emptyList(),
    val searchError: String? = null,
    val isSearching: Boolean = false,
    val addedSuccess: Boolean = false
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val userApi: UserApi,
    private val contactDao: ContactDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            contactDao.getAllContacts().collect { entities ->
                val items = entities.map { entity ->
                    ContactItem(
                        id = entity.id,
                        uniqueId = entity.uniqueId,
                        displayName = entity.displayName,
                        isOnline = false
                    )
                }
                _uiState.value = _uiState.value.copy(contacts = items)
            }
        }
    }

    fun searchUserById(idNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null, addedSuccess = false)
            try {
                val response = userApi.searchUser(idNumber)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // Save to local DB
                    contactDao.insertContact(
                        ContactEntity(
                            id = user.id,
                            uniqueId = user.uniqueId,
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        addedSuccess = true
                    )
                } else {
                    val err = when (response.code()) {
                        404 -> "User not found"
                        400 -> "Cannot add yourself"
                        401 -> "Authentication error. Restart app."
                        else -> "Search failed (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(isSearching = false, searchError = err)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = "Network error: ${e.message}"
                )
            }
        }
    }
}
