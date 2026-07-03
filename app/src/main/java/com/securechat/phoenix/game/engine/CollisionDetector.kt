package com.securechat.phoenix.game.engine

/**
 * Handles all collision detection logic for the game.
 */
object CollisionDetector {

    /**
     * Check if the bird collides with any pillar.
     */
    fun checkPillarCollision(bird: PhoenixBird, pillars: List<Pillar>, screenHeight: Float): Boolean {
        val birdHitbox = bird.getHitbox()
        return pillars.any { pillar ->
            birdHitbox.intersects(pillar.getTopHitbox()) ||
                    birdHitbox.intersects(pillar.getBottomHitbox(screenHeight))
        }
    }

    /**
     * Check if the bird has hit the ground or ceiling.
     */
    fun checkBoundsCollision(bird: PhoenixBird, screenHeight: Float): Boolean {
        return bird.y + PhoenixBird.SIZE > screenHeight || bird.y < 0
    }

    /**
     * Check if the bird has passed a pillar (for scoring).
     * A pillar is "passed" when the bird's x position is past the pillar's right edge.
     */
    fun checkScoring(bird: PhoenixBird, pillar: Pillar): Boolean {
        return !pillar.scored && bird.x > pillar.x + pillar.width
    }
}
