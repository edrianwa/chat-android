package com.securechat.phoenix.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillarTest {

    @Test
    fun `scroll moves pillar left`() {
        val pillar = Pillar(x = 500f, gapTop = 300f, gapBottom = 480f)
        val scrolled = pillar.scroll(5f)
        assertEquals(495f, scrolled.x, 0.001f)
    }

    @Test
    fun `pillar is off screen when fully past left edge`() {
        val pillar = Pillar(x = -80f, gapTop = 300f, gapBottom = 480f, width = 70f)
        assertTrue(pillar.isOffScreen())
    }

    @Test
    fun `pillar is not off screen when partially visible`() {
        val pillar = Pillar(x = -50f, gapTop = 300f, gapBottom = 480f, width = 70f)
        assertFalse(pillar.isOffScreen())
    }

    @Test
    fun `markScored sets scored flag`() {
        val pillar = Pillar(x = 100f, gapTop = 300f, gapBottom = 480f, scored = false)
        val scored = pillar.markScored()
        assertTrue(scored.scored)
    }

    @Test
    fun `top hitbox covers from screen top to gap`() {
        val pillar = Pillar(x = 100f, gapTop = 300f, gapBottom = 480f, width = 70f)
        val hitbox = pillar.getTopHitbox()
        assertEquals(100f, hitbox.left, 0.001f)
        assertEquals(0f, hitbox.top, 0.001f)
        assertEquals(170f, hitbox.right, 0.001f)
        assertEquals(300f, hitbox.bottom, 0.001f)
    }

    @Test
    fun `bottom hitbox covers from gap to screen bottom`() {
        val pillar = Pillar(x = 100f, gapTop = 300f, gapBottom = 480f, width = 70f)
        val hitbox = pillar.getBottomHitbox(800f)
        assertEquals(100f, hitbox.left, 0.001f)
        assertEquals(480f, hitbox.top, 0.001f)
        assertEquals(170f, hitbox.right, 0.001f)
        assertEquals(800f, hitbox.bottom, 0.001f)
    }
}
