package com.securechat.phoenix.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoenixBirdTest {

    @Test
    fun `initial bird has zero velocity`() {
        val bird = PhoenixBird(y = 300f)
        assertEquals(0f, bird.velocity, 0.001f)
    }

    @Test
    fun `gravity increases velocity downward each frame`() {
        val bird = PhoenixBird(y = 300f, velocity = 0f)
        val updated = bird.applyGravity()
        assertEquals(PhoenixBird.GRAVITY, updated.velocity, 0.001f)
        assertTrue(updated.y > bird.y)
    }

    @Test
    fun `gravity accumulates over multiple frames`() {
        var bird = PhoenixBird(y = 300f, velocity = 0f)
        repeat(5) { bird = bird.applyGravity() }
        val expectedVelocity = PhoenixBird.GRAVITY * 5
        assertEquals(expectedVelocity, bird.velocity, 0.001f)
    }

    @Test
    fun `velocity is clamped to max fall speed`() {
        val bird = PhoenixBird(y = 300f, velocity = PhoenixBird.MAX_FALL_SPEED - 0.1f)
        val updated = bird.applyGravity()
        assertTrue(updated.velocity <= PhoenixBird.MAX_FALL_SPEED)
    }

    @Test
    fun `flap sets upward impulse`() {
        val bird = PhoenixBird(y = 300f, velocity = 5f)
        val flapped = bird.flap()
        assertEquals(PhoenixBird.TAP_IMPULSE, flapped.velocity, 0.001f)
    }

    @Test
    fun `flap resets wing animation`() {
        val bird = PhoenixBird(y = 300f, wingFrame = 2, wingAnimTimer = 4)
        val flapped = bird.flap()
        assertEquals(0, flapped.wingFrame)
        assertEquals(0, flapped.wingAnimTimer)
    }

    @Test
    fun `bird moves upward after flap`() {
        val bird = PhoenixBird(y = 300f, velocity = 0f)
        val flapped = bird.flap()
        val afterGravity = flapped.applyGravity()
        assertTrue(afterGravity.y < bird.y)
    }

    @Test
    fun `rotation is positive when falling`() {
        val bird = PhoenixBird(y = 300f, velocity = 5f)
        val updated = bird.applyGravity()
        assertTrue(updated.rotation > 0)
    }

    @Test
    fun `rotation is negative when rising`() {
        val bird = PhoenixBird(y = 300f, velocity = PhoenixBird.TAP_IMPULSE)
        val updated = bird.applyGravity()
        assertTrue(updated.rotation < 0)
    }

    @Test
    fun `wing frame cycles correctly`() {
        var bird = PhoenixBird(y = 300f)
        // Advance through one full wing animation cycle
        repeat(PhoenixBird.WING_ANIM_SPEED) {
            bird = bird.applyGravity()
        }
        assertEquals(1, bird.wingFrame)
    }

    @Test
    fun `hitbox is smaller than visual size`() {
        val bird = PhoenixBird(x = 100f, y = 200f)
        val hitbox = bird.getHitbox()
        assertTrue(hitbox.left > bird.x)
        assertTrue(hitbox.top > bird.y)
        assertTrue(hitbox.right < bird.x + PhoenixBird.SIZE)
        assertTrue(hitbox.bottom < bird.y + PhoenixBird.SIZE)
    }

    @Test
    fun `velocity cannot exceed max rise speed`() {
        // Flap multiple times rapidly
        var bird = PhoenixBird(y = 300f, velocity = PhoenixBird.MAX_RISE_SPEED + 0.1f)
        bird = bird.applyGravity()
        assertTrue(bird.velocity >= PhoenixBird.MAX_RISE_SPEED)
    }
}
