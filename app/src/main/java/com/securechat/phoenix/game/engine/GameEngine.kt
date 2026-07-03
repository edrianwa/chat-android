package com.securechat.phoenix.game.engine

import kotlin.random.Random

/**
 * Core game engine that updates the game state each frame.
 * Pure functions — no side effects, easily testable.
 */
object GameEngine {

    /**
     * Initialize the game state for a given screen size.
     */
    fun initialize(screenWidth: Float, screenHeight: Float, highScore: Int = 0): GameState {
        return GameState(
            phase = GamePhase.READY,
            bird = PhoenixBird(y = screenHeight / 2f - PhoenixBird.SIZE / 2f),
            pillars = emptyList(),
            score = 0,
            highScore = highScore,
            frameCount = 0,
            scrollSpeed = GameState.INITIAL_SCROLL_SPEED
        )
    }

    /**
     * Process a tap event based on current game phase.
     */
    fun onTap(state: GameState): GameState {
        return when (state.phase) {
            GamePhase.READY -> state.copy(
                phase = GamePhase.PLAYING,
                bird = state.bird.flap()
            )
            GamePhase.PLAYING -> state.copy(
                bird = state.bird.flap()
            )
            GamePhase.DEAD -> state // Handled separately via restart
        }
    }

    /**
     * Restart the game from death screen.
     */
    fun restart(state: GameState, screenWidth: Float, screenHeight: Float): GameState {
        return initialize(screenWidth, screenHeight, state.highScore)
    }

    /**
     * Main game loop tick — called every frame during PLAYING phase.
     */
    fun update(state: GameState, screenWidth: Float, screenHeight: Float): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val newFrameCount = state.frameCount + 1

        // Update bird physics
        val updatedBird = state.bird.applyGravity()

        // Scroll existing pillars
        var updatedPillars = state.pillars.map { it.scroll(state.scrollSpeed) }

        // Remove off-screen pillars
        updatedPillars = updatedPillars.filter { !it.isOffScreen() }

        // Spawn new pillars at interval
        if (newFrameCount % GameState.PILLAR_SPAWN_INTERVAL == 0L) {
            val newPillar = spawnPillar(screenWidth, screenHeight)
            updatedPillars = updatedPillars + newPillar
        }

        // Check scoring
        var newScore = state.score
        updatedPillars = updatedPillars.map { pillar ->
            if (CollisionDetector.checkScoring(updatedBird, pillar)) {
                newScore++
                pillar.markScored()
            } else {
                pillar
            }
        }

        // Update high score
        val newHighScore = maxOf(state.highScore, newScore)

        // Check collisions
        val hitPillar = CollisionDetector.checkPillarCollision(updatedBird, updatedPillars, screenHeight)
        val hitBounds = CollisionDetector.checkBoundsCollision(updatedBird, screenHeight)

        if (hitPillar || hitBounds) {
            return state.copy(
                phase = GamePhase.DEAD,
                bird = updatedBird,
                score = newScore,
                highScore = newHighScore,
                frameCount = newFrameCount
            )
        }

        // Gradually increase speed
        val newSpeed = (state.scrollSpeed + GameState.SPEED_INCREMENT)
            .coerceAtMost(GameState.MAX_SCROLL_SPEED)

        // Update background parallax offsets
        val newBgOffset = (state.backgroundOffset + newSpeed * 0.3f) % screenWidth
        val newMidOffset = (state.midgroundOffset + newSpeed * 0.6f) % screenWidth

        return state.copy(
            bird = updatedBird,
            pillars = updatedPillars,
            score = newScore,
            highScore = newHighScore,
            frameCount = newFrameCount,
            scrollSpeed = newSpeed,
            backgroundOffset = newBgOffset,
            midgroundOffset = newMidOffset
        )
    }

    /**
     * Spawn a new pillar at the right edge of the screen.
     */
    private fun spawnPillar(screenWidth: Float, screenHeight: Float): Pillar {
        val minGapTop = GameState.MIN_PILLAR_HEIGHT
        val maxGapTop = screenHeight - GameState.MIN_PILLAR_HEIGHT - GameState.PILLAR_GAP_SIZE
        val gapTop = Random.nextFloat() * (maxGapTop - minGapTop) + minGapTop
        val gapBottom = gapTop + GameState.PILLAR_GAP_SIZE

        return Pillar(
            x = screenWidth,
            gapTop = gapTop,
            gapBottom = gapBottom
        )
    }
}
