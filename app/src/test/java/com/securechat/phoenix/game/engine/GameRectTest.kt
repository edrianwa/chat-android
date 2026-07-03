package com.securechat.phoenix.game.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRectTest {

    @Test
    fun `overlapping rectangles intersect`() {
        val a = GameRect(0f, 0f, 50f, 50f)
        val b = GameRect(25f, 25f, 75f, 75f)
        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
    }

    @Test
    fun `non-overlapping rectangles do not intersect`() {
        val a = GameRect(0f, 0f, 50f, 50f)
        val b = GameRect(100f, 100f, 150f, 150f)
        assertFalse(a.intersects(b))
        assertFalse(b.intersects(a))
    }

    @Test
    fun `adjacent rectangles do not intersect`() {
        val a = GameRect(0f, 0f, 50f, 50f)
        val b = GameRect(50f, 0f, 100f, 50f)
        assertFalse(a.intersects(b))
    }

    @Test
    fun `contained rectangle intersects`() {
        val outer = GameRect(0f, 0f, 100f, 100f)
        val inner = GameRect(25f, 25f, 75f, 75f)
        assertTrue(outer.intersects(inner))
        assertTrue(inner.intersects(outer))
    }

    @Test
    fun `horizontal overlap but vertical separation does not intersect`() {
        val a = GameRect(0f, 0f, 50f, 50f)
        val b = GameRect(25f, 60f, 75f, 110f)
        assertFalse(a.intersects(b))
    }

    @Test
    fun `vertical overlap but horizontal separation does not intersect`() {
        val a = GameRect(0f, 0f, 50f, 50f)
        val b = GameRect(60f, 25f, 110f, 75f)
        assertFalse(a.intersects(b))
    }
}
