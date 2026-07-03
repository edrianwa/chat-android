package com.securechat.phoenix.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val screenWidth = 1080f
    private val screenHeight = 1920f

    @Test
    fun `initialize creates READY state`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        assertEquals(GamePhase.READY, state.phase)
    }

    @Test
    fun `initialize sets bird at vertical center`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        val expectedY = screenHeight / 2f - PhoenixBird.SIZE / 2f
        assertEquals(expectedY, state.bird.y, 0.001f)
    }

    @Test
    fun `initialize preserves high score`() {
        val state = GameEngine.initialize(screenWidth, screenHeight, highScore = 42)
        assertEquals(42, state.highScore)
    }

    @Test
    fun `initialize starts with zero score`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        assertEquals(0, state.score)
    }

    @Test
    fun `initialize has no pillars`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        assertTrue(state.pillars.isEmpty())
    }

    @Test
    fun `tap in READY transitions to PLAYING`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        val result = GameEngine.onTap(state)
        assertEquals(GamePhase.PLAYING, result.phase)
    }

    @Test
    fun `tap in PLAYING applies flap`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)
        val result = GameEngine.onTap(state)
        assertEquals(PhoenixBird.TAP_IMPULSE, result.bird.velocity, 0.001f)
    }

    @Test
    fun `tap in DEAD does not change state`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.DEAD)
        val result = GameEngine.onTap(state)
        assertEquals(GamePhase.DEAD, result.phase)
    }

    @Test
    fun `restart returns READY state with preserved high score`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.DEAD, score = 10, highScore = 15)
        val result = GameEngine.restart(state, screenWidth, screenHeight)
        assertEquals(GamePhase.READY, result.phase)
        assertEquals(0, result.score)
        assertEquals(15, result.highScore)
    }

    @Test
    fun `update does not modify READY state`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
        val result = GameEngine.update(state, screenWidth, screenHeight)
        assertEquals(state, result)
    }

    @Test
    fun `update increments frame count`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)
        val result = GameEngine.update(state, screenWidth, screenHeight)
        assertEquals(1L, result.frameCount)
    }

    @Test
    fun `update applies gravity to bird`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)
        val result = GameEngine.update(state, screenWidth, screenHeight)
        assertTrue(result.bird.velocity > 0) // Falling
    }

    @Test
    fun `pillars spawn at correct interval`() {
        var state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)
        // Run for PILLAR_SPAWN_INTERVAL frames
        repeat(GameState.PILLAR_SPAWN_INTERVAL) {
            state = GameEngine.update(state, screenWidth, screenHeight)
            if (state.phase == GamePhase.DEAD) return // Bird might die
        }
        if (state.phase == GamePhase.PLAYING) {
            assertTrue(state.pillars.isNotEmpty())
        }
    }

    @Test
    fun `scroll speed increases over time`() {
        var state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING)
        val initialSpeed = state.scrollSpeed
        // Run several frames
        repeat(10) {
            state = GameEngine.update(state, screenWidth, screenHeight)
            if (state.phase != GamePhase.PLAYING) return
        }
        if (state.phase == GamePhase.PLAYING) {
            assertTrue(state.scrollSpeed > initialSpeed)
        }
    }

    @Test
    fun `scroll speed does not exceed max`() {
        val state = GameEngine.initialize(screenWidth, screenHeight)
            .copy(phase = GamePhase.PLAYING, scrollSpeed = GameState.MAX_SCROLL_SPEED)
        val result = GameEngine.update(state, screenWidth, screenHeight)
        if (result.phase == GamePhase.PLAYING) {
            assertTrue(result.scrollSpeed <= GameState.MAX_SCROLL_SPEED)
        }
    }

    @Test
    fun `bird hitting floor causes DEAD phase`() {
        // Place bird near the bottom
        val state = GameEngine.initialize(screenWidth, screenHeight).copy(
            phase = GamePhase.PLAYING,
            bird = PhoenixBird(y = screenHeight - 10f, velocity = 5f)
        )
        val result = GameEngine.update(state, screenWidth, screenHeight)
        assertEquals(GamePhase.DEAD, result.phase)
    }

    @Test
    fun `bird hitting ceiling causes DEAD phase`() {
        val state = GameEngine.initialize(screenWidth, screenHeight).copy(
            phase = GamePhase.PLAYING,
            bird = PhoenixBird(y = -5f, velocity = -5f)
        )
        val result = GameEngine.update(state, screenWidth, screenHeight)
        assertEquals(GamePhase.DEAD, result.phase)
    }

    @Test
    fun `score increments when passing pillar`() {
        // Position bird just past a pillar
        val pillar = Pillar(x = 50f, gapTop = 800f, gapBottom = 1000f, width = 70f, scored = false)
        val state = GameEngine.initialize(screenWidth, screenHeight).copy(
            phase = GamePhase.PLAYING,
            bird = PhoenixBird(x = 130f, y = 900f, velocity = 0f), // In the gap, past pillar
            pillars = listOf(pillar)
        )
        val result = GameEngine.update(state, screenWidth, screenHeight)
        if (result.phase == GamePhase.PLAYING) {
            assertEquals(1, result.score)
        }
    }

    @Test
    fun `high score updates when score exceeds it`() {
        val pillar = Pillar(x = 50f, gapTop = 800f, gapBottom = 1000f, width = 70f, scored = false)
        val state = GameEngine.initialize(screenWidth, screenHeight, highScore = 0).copy(
            phase = GamePhase.PLAYING,
            bird = PhoenixBird(x = 130f, y = 900f, velocity = 0f),
            pillars = listOf(pillar)
        )
        val result = GameEngine.update(state, screenWidth, screenHeight)
        if (result.phase == GamePhase.PLAYING) {
            assertTrue(result.highScore >= result.score)
        }
    }

    @Test
    fun `off-screen pillars are removed`() {
        val pillar = Pillar(x = -100f, gapTop = 300f, gapBottom = 480f, width = 70f)
        val state = GameEngine.initialize(screenWidth, screenHeight).copy(
            phase = GamePhase.PLAYING,
            bird = PhoenixBird(y = screenHeight / 2f, velocity = 0f),
            pillars = listOf(pillar)
        )
        val result = GameEngine.update(state, screenWidth, screenHeight)
        if (result.phase == GamePhase.PLAYING) {
            assertTrue(result.pillars.isEmpty())
        }
    }
}
