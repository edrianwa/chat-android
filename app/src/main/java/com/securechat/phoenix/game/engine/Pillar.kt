package com.securechat.phoenix.game.engine

/**
 * A pair of fire pillars (top and bottom) with a gap in between.
 * Scrolls from right to left.
 */
data class Pillar(
    val x: Float,
    val gapTop: Float,      // Y position where the gap starts (top pillar ends here)
    val gapBottom: Float,   // Y position where the gap ends (bottom pillar starts here)
    val width: Float = GameState.PILLAR_WIDTH,
    val scored: Boolean = false  // Whether the bird has passed this pillar
) {
    /**
     * Get the top pillar hitbox (from screen top to gap start).
     */
    fun getTopHitbox(): GameRect {
        return GameRect(
            left = x,
            top = 0f,
            right = x + width,
            bottom = gapTop
        )
    }

    /**
     * Get the bottom pillar hitbox (from gap end to screen bottom).
     */
    fun getBottomHitbox(screenHeight: Float): GameRect {
        return GameRect(
            left = x,
            top = gapBottom,
            right = x + width,
            bottom = screenHeight
        )
    }

    /**
     * Move the pillar to the left by the given speed.
     */
    fun scroll(speed: Float): Pillar {
        return copy(x = x - speed)
    }

    /**
     * Check if the pillar is off-screen (left side).
     */
    fun isOffScreen(): Boolean {
        return x + width < 0
    }

    /**
     * Mark this pillar as scored.
     */
    fun markScored(): Pillar {
        return copy(scored = true)
    }
}
