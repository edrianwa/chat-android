package com.securechat.phoenix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Secure passcode storage backed by Android's encrypted DataStore.
 *
 * Passcodes are hashed using SHA-256 before storage, so the raw
 * passcode is never persisted. The mapping is stored as:
 *   key = "passcode_<hash>" -> value = screenId
 *
 * In production, the DataStore file itself should be encrypted using
 * EncryptedFile from AndroidX Security. This implementation uses
 * standard DataStore with hashed keys as the base security layer.
 */
private val Context.passcodeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "phoenix_passcodes"
)

class SecurePasscodeStore(private val context: Context) {

    companion object {
        private const val KEY_PREFIX = "passcode_"
        private const val HAS_PASSCODES_KEY = "has_passcodes"
    }

    /**
     * Hash a passcode using SHA-256.
     * This ensures the raw passcode is never stored.
     */
    private fun hashPasscode(passcode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(passcode.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Store a passcode-to-screen mapping.
     * The passcode is hashed before being used as the key.
     */
    suspend fun storeMapping(passcode: String, screenId: String) {
        val hash = hashPasscode(passcode)
        context.passcodeDataStore.edit { preferences ->
            preferences[stringPreferencesKey("$KEY_PREFIX$hash")] = screenId
            preferences[stringPreferencesKey(HAS_PASSCODES_KEY)] = "true"
        }
    }

    /**
     * Find the screen mapping for a given passcode.
     * Returns null if no mapping exists for this passcode.
     */
    suspend fun findMapping(passcode: String): PasscodeMapping? {
        val hash = hashPasscode(passcode)
        val key = stringPreferencesKey("$KEY_PREFIX$hash")

        val screenId = context.passcodeDataStore.data
            .map { preferences -> preferences[key] }
            .first()

        return screenId?.let { PasscodeMapping(passcodeHash = hash, screenId = it) }
    }

    /**
     * Check if any passcodes have been configured.
     */
    suspend fun hasStoredPasscodes(): Boolean {
        val key = stringPreferencesKey(HAS_PASSCODES_KEY)
        return context.passcodeDataStore.data
            .map { preferences -> preferences[key] == "true" }
            .first()
    }

    /**
     * Clear all stored passcode data.
     */
    suspend fun clearAll() {
        context.passcodeDataStore.edit { it.clear() }
    }
}
