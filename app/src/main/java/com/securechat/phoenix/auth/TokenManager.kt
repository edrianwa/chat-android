package com.securechat.phoenix.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

/**
 * Manages JWT tokens and user session data.
 * Stored in DataStore (persists across app restarts).
 */
@Singleton
class TokenManager @Inject constructor(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val UNIQUE_ID = stringPreferencesKey("unique_id")
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val ROLE = stringPreferencesKey("role")
    }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN] != null
    }

    val userId: Flow<String?> = context.authDataStore.data.map { it[USER_ID] }
    val uniqueId: Flow<String?> = context.authDataStore.data.map { it[UNIQUE_ID] }
    val displayName: Flow<String?> = context.authDataStore.data.map { it[DISPLAY_NAME] }

    suspend fun getAccessToken(): String? {
        return context.authDataStore.data.first()[ACCESS_TOKEN]
    }

    suspend fun getRefreshToken(): String? {
        return context.authDataStore.data.first()[REFRESH_TOKEN]
    }

    suspend fun getUserId(): String? {
        return context.authDataStore.data.first()[USER_ID]
    }

    suspend fun getUniqueId(): String? {
        return context.authDataStore.data.first()[UNIQUE_ID]
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUser(user: UserData) {
        context.authDataStore.edit { prefs ->
            prefs[USER_ID] = user.id
            prefs[UNIQUE_ID] = user.uniqueId
            prefs[DISPLAY_NAME] = user.displayName
            prefs[ROLE] = user.role
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { it.clear() }
    }
}
