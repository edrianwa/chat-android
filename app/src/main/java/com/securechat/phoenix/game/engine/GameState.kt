package com.securechat.phoenix.game.engine

/**
 * Represents the current phase of the game lifecycle.
 */
enum class GamePhase {
    READY,      // Start screen - waiting for first tap
    PLAYING,    // Active gameplay
    DEAD        // Death screen - showing score
}

/**
 * Core game state holding all mutable game data.
 * Updated each frame by the game loop.
 */
data class GameState(
    val phase: GamePhase = GamePhase.READY,
    val bird: PhoenixBird = PhoenixBird(),
    val pillars: List<Pillar> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val frameCount: Long = 0,
    val scrollSpeed: Float = INITIAL_SCROLL_SPEED,
    val backgroundOffset: Float = 0f,
    val midgroundOffset: Float = 0f
) {
    companion object {
        const val INITIAL_SCROLL_SPEED = 4f
        const val MAX_SCROLL_SPEED = 7f
        const val SPEED_INCREMENT = 0.001f
        const val PILLAR_SPAWN_INTERVAL = 90 // frames between pillar spawns
        const val PILLAR_GAP_SIZE = 180f // vertical gap between top and bottom pillars
        const val PILLAR_WIDTH = 70f
        const val MIN_PILLAR_HEIGHT = 80f
    }
}
