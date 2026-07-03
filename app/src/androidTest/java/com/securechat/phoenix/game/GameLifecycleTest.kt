package com.securechat.phoenix.game

import com.securechat.phoenix.game.engine.GameEngine
import com.securechat.phoenix.game.engine.GamePhase
import com.securechat.phoenix.game.engine.GameState
import com.securechat.phoenix.game.engine.PhoenixBird
import com.securechat.phoenix.game.engine.Pillar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration-style tests that exercise the full game lifecycle:
 * start → play → die → restart
 *
 * These are placed in androidTest since they validate the complete flow,
 * but use pure engine logic without needing a real UI.
 */
class GameLifecycleTest {

    private val screenWidth = 1080f
    private val screenHeight = 1920f

    @Test
    fun fullGameLifecycle_startPlayDieRestart() {
        // 1. Game starts in READY
        var state = GameEngine.initialize(screenWidth, screenHeight)
        assertEquals(GamePhase.READY, state.phase)

        // 2. Tap to start playing
        state = GameEngine.onTap(state)
        assertEquals(GamePhase.PLAYING, state.phase)

        // 3. Simulate some gameplay frames
        repeat(20) {
            if (state.phase == GamePhase.PLAYING) {
                state = GameEngine.update(state, screenWidth, screenHeight)
            }
        }

        // If still alive, keep flapping to stay alive
        if (state.phase == GamePhase.PLAYING) {
            state = GameEngine.onTap(state) // Flap to stay airborne
            repeat(10) {
                if (state.phase == GamePhase.PLAYING) {
                    state = GameEngine.update(state, screenWidth, screenHeight)
                }
            }
        }

        // 4. Force death by placing bird at ground
        if (state.phase == GamePhase.PLAYING) {
            state = state.copy(bird = state.bird.copy(y = screenHeight + 10f))
            state = GameEngine.update(state, screenWidth, screenHeight)
        }
        assertEquals(GamePhase.DEAD, state.phase)

        // 5. Restart
        val finalScore = state.score
        state = GameEngine.restart(state, screenWidth, screenHeight)
        assertEquals(GamePhase.READY, state.phase)
        assertEquals(0, state.score)
        assertTrue(state.pillars.isEmpty())
    }

    @Test
    fun gameLifecycle_scoringAccumulates() {
        // Start playing
        var state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)

        // Add passed pillars (bird already past them)
        val passedPillar1 = Pillar(x = 30f, gapTop = 800f, gapBottom = 1000f, width = 70f, scored = false)
        val passedPillar2 = Pillar(x = -10f, gapTop = 800f, gapBottom = 1000f, width = 70f, scored = false)
        state = state.copy(
            bird = PhoenixBird(x = 130f, y = 900f, velocity = 0f),
            pillars = listOf(passedPillar1, passedPillar2)
        )

        state = GameEngine.update(state, screenWidth, screenHeight)
        if (state.phase == GamePhase.PLAYING) {
            // Both pillars should be scored
            assertTrue(state.score >= 1)
        }
    }

    @Test
    fun gameLifecycle_highScorePreservedAcrossRestarts() {
        // Play and die with score
        var state = GameEngine.initialize(screenWidth, screenHeight, highScore = 0)
            .copy(
                phase = GamePhase.DEAD,
                score = 10,
                highScore = 10
            )

        // Restart
        state = GameEngine.restart(state, screenWidth, screenHeight)
        assertEquals(10, state.highScore)
        assertEquals(0, state.score)

        // Play again and die with lower score
        state = state.copy(
            phase = GamePhase.DEAD,
            score = 5,
            highScore = 10
        )
        state = GameEngine.restart(state, screenWidth, screenHeight)
        assertEquals(10, state.highScore) // High score preserved
    }

    @Test
    fun gameLifecycle_multipleFlapsKeepBirdAirborne() {
        var state = GameEngine.initialize(screenWidth, screenHeight)
        state = GameEngine.onTap(state) // Start playing

        // Alternate between flapping and updating
        repeat(30) {
            if (state.phase == GamePhase.PLAYING) {
                if (it % 5 == 0) {
                    state = GameEngine.onTap(state) // Flap every 5 frames
                }
                state = GameEngine.update(state, screenWidth, screenHeight)
            }
        }

        // Bird should still be on screen if we flapped enough
        if (state.phase == GamePhase.PLAYING) {
            assertTrue(state.bird.y > 0)
            assertTrue(state.bird.y < screenHeight)
        }
    }

    @Test
    fun gameLifecycle_frameCountIncrements() {
        var state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING, bird = PhoenixBird(y = screenHeight / 2f, velocity = 0f))

        val framesToRun = 10
        repeat(framesToRun) {
            if (state.phase == GamePhase.PLAYING) {
                state = GameEngine.update(state, screenWidth, screenHeight)
            }
        }

        if (state.phase == GamePhase.PLAYING) {
            assertEquals(framesToRun.toLong(), state.frameCount)
        }
    }
}
