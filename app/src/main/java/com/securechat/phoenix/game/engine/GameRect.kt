package com.securechat.phoenix.game.engine

/**
 * Simple rectangle for collision detection.
 */
data class GameRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    /**
     * Check if this rectangle intersects with another.
     * Used for AABB collision detection.
     */
    fun intersects(other: GameRect): Boolean {
        return left < other.right &&
                right > other.left &&
                top < other.bottom &&
                bottom > other.top
    }
}
