package com.securechat.phoenix.game.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionDetectorTest {

    private val screenHeight = 800f

    @Test
    fun `bird in gap does not collide with pillar`() {
        val bird = PhoenixBird(x = 100f, y = 350f) // In the gap
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f)
        assertFalse(CollisionDetector.checkPillarCollision(bird, listOf(pillar), screenHeight))
    }

    @Test
    fun `bird hitting top pillar is detected`() {
        val bird = PhoenixBird(x = 100f, y = 50f) // Above gap
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f)
        assertTrue(CollisionDetector.checkPillarCollision(bird, listOf(pillar), screenHeight))
    }

    @Test
    fun `bird hitting bottom pillar is detected`() {
        val bird = PhoenixBird(x = 100f, y = 500f) // Below gap
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f)
        assertTrue(CollisionDetector.checkPillarCollision(bird, listOf(pillar), screenHeight))
    }

    @Test
    fun `bird past pillar x does not collide`() {
        val bird = PhoenixBird(x = 200f, y = 50f) // Past the pillar
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f, width = 70f)
        assertFalse(CollisionDetector.checkPillarCollision(bird, listOf(pillar), screenHeight))
    }

    @Test
    fun `bird before pillar x does not collide`() {
        val bird = PhoenixBird(x = 10f, y = 50f) // Before the pillar
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f)
        assertFalse(CollisionDetector.checkPillarCollision(bird, listOf(pillar), screenHeight))
    }

    @Test
    fun `bird at bottom of screen hits bounds`() {
        val bird = PhoenixBird(x = 100f, y = screenHeight - 10f)
        assertTrue(CollisionDetector.checkBoundsCollision(bird, screenHeight))
    }

    @Test
    fun `bird at top of screen hits bounds`() {
        val bird = PhoenixBird(x = 100f, y = -5f)
        assertTrue(CollisionDetector.checkBoundsCollision(bird, screenHeight))
    }

    @Test
    fun `bird in middle does not hit bounds`() {
        val bird = PhoenixBird(x = 100f, y = 400f)
        assertFalse(CollisionDetector.checkBoundsCollision(bird, screenHeight))
    }

    @Test
    fun `scoring detected when bird passes pillar`() {
        val bird = PhoenixBird(x = 200f, y = 350f)
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f, width = 70f, scored = false)
        assertTrue(CollisionDetector.checkScoring(bird, pillar))
    }

    @Test
    fun `scoring not triggered for already scored pillar`() {
        val bird = PhoenixBird(x = 200f, y = 350f)
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f, width = 70f, scored = true)
        assertFalse(CollisionDetector.checkScoring(bird, pillar))
    }

    @Test
    fun `scoring not triggered when bird is before pillar`() {
        val bird = PhoenixBird(x = 50f, y = 350f)
        val pillar = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f, width = 70f, scored = false)
        assertFalse(CollisionDetector.checkScoring(bird, pillar))
    }

    @Test
    fun `multiple pillars only collide with nearest`() {
        val bird = PhoenixBird(x = 100f, y = 350f) // In gap of first pillar
        val pillar1 = Pillar(x = 90f, gapTop = 300f, gapBottom = 480f)
        val pillar2 = Pillar(x = 300f, gapTop = 200f, gapBottom = 380f)
        assertFalse(CollisionDetector.checkPillarCollision(bird, listOf(pillar1, pillar2), screenHeight))
    }

    @Test
    fun `empty pillar list has no collision`() {
        val bird = PhoenixBird(x = 100f, y = 350f)
        assertFalse(CollisionDetector.checkPillarCollision(bird, emptyList(), screenHeight))
    }
}
