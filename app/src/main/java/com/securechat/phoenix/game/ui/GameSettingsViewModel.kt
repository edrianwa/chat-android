package com.securechat.phoenix.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.game.data.GamePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameSettingsState(
    val soundEnabled: Boolean = true,
    val difficulty: Int = 1,
    val highScore: Int = 0
)

@HiltViewModel
class GameSettingsViewModel @Inject constructor(
    private val gamePreferences: GamePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(GameSettingsState())
    val state: StateFlow<GameSettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.value = GameSettingsState(
                soundEnabled = gamePreferences.soundEnabled.first(),
                difficulty = gamePreferences.difficulty.first(),
                highScore = gamePreferences.highScore.first()
            )
        }
    }

    fun onSoundToggle(enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
        viewModelScope.launch { gamePreferences.setSoundEnabled(enabled) }
    }

    fun onDifficultyChange(level: Int) {
        _state.value = _state.value.copy(difficulty = level)
        viewModelScope.launch { gamePreferences.setDifficulty(level) }
    }

    fun onResetHighScore() {
        _state.value = _state.value.copy(highScore = 0)
        viewModelScope.launch { gamePreferences.resetHighScore() }
    }
}
