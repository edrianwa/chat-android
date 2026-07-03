package com.securechat.phoenix.game.engine

/**
 * The phoenix bird character with tap-to-fly gravity physics.
 * Uses simple Euler integration for position updates.
 */
data class PhoenixBird(
    val x: Float = INITIAL_X,
    val y: Float = 0f, // Will be set to center on first frame
    val velocity: Float = 0f,
    val rotation: Float = 0f,
    val wingFrame: Int = 0,
    val wingAnimTimer: Int = 0
) {
    companion object {
        const val INITIAL_X = 100f
        const val SIZE = 40f
        const val GRAVITY = 0.6f
        const val TAP_IMPULSE = -10f
        const val MAX_FALL_SPEED = 12f
        const val MAX_RISE_SPEED = -12f
        const val ROTATION_FALLING = 30f   // degrees when falling
        const val ROTATION_RISING = -20f   // degrees when rising
        const val WING_FRAME_COUNT = 3
        const val WING_ANIM_SPEED = 6 // frames per wing cycle step
        const val HITBOX_PADDING = 4f // pixels of forgiveness on collision
    }

    /**
     * Apply gravity and clamp velocity.
     */
    fun applyGravity(): PhoenixBird {
        val newVelocity = (velocity + GRAVITY).coerceIn(MAX_RISE_SPEED, MAX_FALL_SPEED)
        val newY = y + newVelocity
        val newRotation = if (newVelocity > 0) {
            (newVelocity / MAX_FALL_SPEED) * ROTATION_FALLING
        } else {
            (newVelocity / MAX_RISE_SPEED) * ROTATION_RISING
        }
        // Animate wings
        val newTimer = wingAnimTimer + 1
        val newFrame = if (newTimer >= WING_ANIM_SPEED) {
            (wingFrame + 1) % WING_FRAME_COUNT
        } else {
            wingFrame
        }
        return copy(
            y = newY,
            velocity = newVelocity,
            rotation = newRotation,
            wingFrame = newFrame,
            wingAnimTimer = if (newTimer >= WING_ANIM_SPEED) 0 else newTimer
        )
    }

    /**
     * Apply an upward impulse (tap/flap).
     */
    fun flap(): PhoenixBird {
        return copy(
            velocity = TAP_IMPULSE,
            wingFrame = 0,
            wingAnimTimer = 0
        )
    }

    /**
     * Get the hitbox rectangle for collision detection.
     * Slightly smaller than visual size for fairness.
     */
    fun getHitbox(): GameRect {
        val padding = HITBOX_PADDING
        return GameRect(
            left = x + padding,
            top = y + padding,
            right = x + SIZE - padding,
            bottom = y + SIZE - padding
        )
    }
}
