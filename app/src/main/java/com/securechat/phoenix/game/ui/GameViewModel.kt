package com.securechat.phoenix.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.game.data.GamePreferences
import com.securechat.phoenix.game.sound.GameSoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameScreenState(
    val highScore: Int = 0,
    val soundEnabled: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gamePreferences: GamePreferences,
    private val soundManager: GameSoundManager
) : ViewModel() {

    private val _screenState = MutableStateFlow(GameScreenState())
    val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val highScore = gamePreferences.highScore.first()
            val soundEnabled = gamePreferences.soundEnabled.first()
            _screenState.value = GameScreenState(
                highScore = highScore,
                soundEnabled = soundEnabled,
                isLoading = false
            )
        }
    }

    fun onScoreUpdate(score: Int) {
        val currentHigh = _screenState.value.highScore
        if (score > currentHigh) {
            _screenState.value = _screenState.value.copy(highScore = score)
            viewModelScope.launch {
                gamePreferences.saveHighScore(score)
            }
        }
    }

    fun onGameEvent(event: GameEvent) {
        if (!_screenState.value.soundEnabled) return
        when (event) {
            GameEvent.Flap -> soundManager.playFlap()
            GameEvent.ScorePoint -> soundManager.playScore()
            GameEvent.Crash -> soundManager.playCrash()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
