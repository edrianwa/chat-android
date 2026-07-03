package com.securechat.phoenix.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.securechat.phoenix.game.engine.GameEngine
import com.securechat.phoenix.game.engine.GamePhase
import com.securechat.phoenix.game.engine.GameState
import com.securechat.phoenix.game.renderer.GameRenderer
import com.securechat.phoenix.game.renderer.ScoreRenderer
import kotlinx.coroutines.delay

/**
 * The main game composable that handles the game loop, input,
 * and rendering for the Flying Phoenix game.
 */
@Composable
fun FlyingPhoenixGame(
    highScore: Int,
    onScoreUpdate: (Int) -> Unit,
    onGameEvent: (GameEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var gameState by remember { mutableStateOf(GameState()) }
    var isInitialized by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    // Initialize game once we know the canvas size
    LaunchedEffect(canvasSize, highScore) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            if (!isInitialized) {
                gameState = GameEngine.initialize(
                    screenWidth = canvasSize.width.toFloat(),
                    screenHeight = canvasSize.height.toFloat(),
                    highScore = highScore
                )
                isInitialized = true
            }
        }
    }

    // Game loop — runs at ~60fps during PLAYING phase
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.PLAYING) {
            while (gameState.phase == GamePhase.PLAYING) {
                val previousScore = gameState.score
                gameState = GameEngine.update(
                    gameState,
                    canvasSize.width.toFloat(),
                    canvasSize.height.toFloat()
                )

                // Notify score changes
                if (gameState.score > previousScore) {
                    onGameEvent(GameEvent.ScorePoint)
                    onScoreUpdate(gameState.score)
                }

                // Notify death
                if (gameState.phase == GamePhase.DEAD) {
                    onGameEvent(GameEvent.Crash)
                    onScoreUpdate(gameState.score)
                }

                delay(16L) // ~60fps
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTapGestures {
                    when (gameState.phase) {
                        GamePhase.READY -> {
                            gameState = GameEngine.onTap(gameState)
                            onGameEvent(GameEvent.Flap)
                        }
                        GamePhase.PLAYING -> {
                            gameState = GameEngine.onTap(gameState)
                            onGameEvent(GameEvent.Flap)
                        }
                        GamePhase.DEAD -> {
                            gameState = GameEngine.restart(
                                gameState,
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat()
                            )
                        }
                    }
                }
            }
    ) {
        if (!isInitialized) return@Canvas

        // Render game world
        GameRenderer.render(this, gameState)

        // Render overlays based on phase
        when (gameState.phase) {
            GamePhase.READY -> {
                ScoreRenderer.drawReadyOverlay(this, textMeasurer)
            }
            GamePhase.PLAYING -> {
                ScoreRenderer.drawScore(this, textMeasurer, gameState.score)
            }
            GamePhase.DEAD -> {
                ScoreRenderer.drawDeathOverlay(
                    drawScope = this,
                    textMeasurer = textMeasurer,
                    score = gameState.score,
                    highScore = gameState.highScore,
                    isNewHighScore = gameState.score >= gameState.highScore && gameState.score > 0
                )
            }
        }
    }
}

/**
 * Events emitted by the game for sound effects and analytics.
 */
sealed class GameEvent {
    data object Flap : GameEvent()
    data object ScorePoint : GameEvent()
    data object Crash : GameEvent()
}
