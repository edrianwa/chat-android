package com.securechat.phoenix.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "phoenix_game_prefs"
)

/**
 * Persists game settings and high scores using DataStore.
 */
class GamePreferences(private val context: Context) {

    companion object {
        private val HIGH_SCORE_KEY = intPreferencesKey("high_score")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val DIFFICULTY_KEY = intPreferencesKey("difficulty")
    }

    val highScore: Flow<Int> = context.gameDataStore.data
        .map { it[HIGH_SCORE_KEY] ?: 0 }

    val soundEnabled: Flow<Boolean> = context.gameDataStore.data
        .map { it[SOUND_ENABLED_KEY] ?: true }

    val difficulty: Flow<Int> = context.gameDataStore.data
        .map { it[DIFFICULTY_KEY] ?: 1 } // 0=easy, 1=normal, 2=hard

    suspend fun saveHighScore(score: Int) {
        context.gameDataStore.edit { prefs ->
            val current = prefs[HIGH_SCORE_KEY] ?: 0
            if (score > current) {
                prefs[HIGH_SCORE_KEY] = score
            }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.gameDataStore.edit { it[SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun setDifficulty(level: Int) {
        context.gameDataStore.edit { it[DIFFICULTY_KEY] = level }
    }

    suspend fun resetHighScore() {
        context.gameDataStore.edit { it[HIGH_SCORE_KEY] = 0 }
    }
}
