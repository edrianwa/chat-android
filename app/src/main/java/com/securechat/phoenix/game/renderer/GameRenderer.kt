package com.securechat.phoenix.game.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import com.securechat.phoenix.game.engine.GameState
import com.securechat.phoenix.game.engine.PhoenixBird
import com.securechat.phoenix.game.engine.Pillar
import kotlin.math.sin

/**
 * Renders all game visuals using Compose Canvas DrawScope.
 * Draws parallax background, fire pillars, and the phoenix character.
 */
object GameRenderer {

    // Color palette
    private val skyGradientTop = Color(0xFF0D0221)
    private val skyGradientBottom = Color(0xFF2D1B69)
    private val cloudColor = Color(0x33FF6B35)
    private val midCloudColor = Color(0x44FF8C42)

    // Pillar colors
    private val pillarGradientStart = Color(0xFFFF4500)
    private val pillarGradientEnd = Color(0xFFFF8C00)
    private val pillarEdge = Color(0xFFFFD700)
    private val pillarGlow = Color(0x44FF6B35)

    // Phoenix colors
    private val phoenixBodyOuter = Color(0xFFFF6B35)
    private val phoenixBodyInner = Color(0xFFFFB347)
    private val phoenixBelly = Color(0xFFFFF3E0)
    private val phoenixWing = Color(0xFFE85D04)
    private val phoenixWingTip = Color(0xFFFFD700)
    private val phoenixEye = Color(0xFF1A1A2E)
    private val phoenixBeak = Color(0xFFFF4500)

    /**
     * Render the entire game frame.
     */
    fun render(drawScope: DrawScope, state: GameState) {
        with(drawScope) {
            drawBackground(state)
            drawPillars(state.pillars)
            drawPhoenix(state.bird)
        }
    }

    /**
     * Draw the parallax scrolling background.
     */
    private fun DrawScope.drawBackground(state: GameState) {
        val width = size.width
        val height = size.height

        // Sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyGradientTop, skyGradientBottom),
                startY = 0f,
                endY = height
            )
        )

        // Background layer: distant fire clouds (slow scroll)
        drawParallaxClouds(
            offset = state.backgroundOffset,
            color = cloudColor,
            yBase = height * 0.2f,
            cloudSize = 120f,
            spacing = 300f
        )

        // Midground layer: closer embers/clouds (medium scroll)
        drawParallaxClouds(
            offset = state.midgroundOffset,
            color = midCloudColor,
            yBase = height * 0.6f,
            cloudSize = 80f,
            spacing = 220f
        )

        // Ground fire line
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF4500), Color(0xFF8B0000)),
                startY = height - 4f,
                endY = height
            ),
            topLeft = Offset(0f, height - 4f),
            size = Size(width, 4f)
        )
    }

    /**
     * Draw a row of parallax cloud shapes.
     */
    private fun DrawScope.drawParallaxClouds(
        offset: Float,
        color: Color,
        yBase: Float,
        cloudSize: Float,
        spacing: Float
    ) {
        val width = size.width
        val count = (width / spacing).toInt() + 2

        for (i in 0..count) {
            val baseX = i * spacing - offset % spacing
            val yOffset = sin(i * 1.5f) * 30f
            drawOval(
                color = color,
                topLeft = Offset(baseX - cloudSize / 2, yBase + yOffset),
                size = Size(cloudSize, cloudSize * 0.4f)
            )
        }
    }

    /**
     * Draw fire pillars (obstacles).
     */
    private fun DrawScope.drawPillars(pillars: List<Pillar>) {
        val screenHeight = size.height
        pillars.forEach { pillar ->
            drawFirePillar(pillar, screenHeight)
        }
    }

    /**
     * Draw a single fire pillar pair (top and bottom).
     */
    private fun DrawScope.drawFirePillar(pillar: Pillar, screenHeight: Float) {
        val x = pillar.x
        val w = pillar.width

        // Glow effect behind pillars
        drawRoundRect(
            color = pillarGlow,
            topLeft = Offset(x - 6f, 0f),
            size = Size(w + 12f, pillar.gapTop),
            cornerRadius = CornerRadius(8f)
        )
        drawRoundRect(
            color = pillarGlow,
            topLeft = Offset(x - 6f, pillar.gapBottom),
            size = Size(w + 12f, screenHeight - pillar.gapBottom),
            cornerRadius = CornerRadius(8f)
        )

        // Top pillar
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(pillarGradientStart, pillarGradientEnd, pillarGradientStart),
                startX = x,
                endX = x + w
            ),
            topLeft = Offset(x, 0f),
            size = Size(w, pillar.gapTop),
            cornerRadius = CornerRadius(6f)
        )

        // Top pillar cap (ring)
        drawRoundRect(
            color = pillarEdge,
            topLeft = Offset(x - 4f, pillar.gapTop - 20f),
            size = Size(w + 8f, 20f),
            cornerRadius = CornerRadius(4f)
        )

        // Bottom pillar
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(pillarGradientStart, pillarGradientEnd, pillarGradientStart),
                startX = x,
                endX = x + w
            ),
            topLeft = Offset(x, pillar.gapBottom),
            size = Size(w, screenHeight - pillar.gapBottom),
            cornerRadius = CornerRadius(6f)
        )

        // Bottom pillar cap (ring)
        drawRoundRect(
            color = pillarEdge,
            topLeft = Offset(x - 4f, pillar.gapBottom),
            size = Size(w + 8f, 20f),
            cornerRadius = CornerRadius(4f)
        )
    }

    /**
     * Draw the phoenix bird with wing animation.
     */
    private fun DrawScope.drawPhoenix(bird: PhoenixBird) {
        val cx = bird.x + PhoenixBird.SIZE / 2
        val cy = bird.y + PhoenixBird.SIZE / 2
        val birdSize = PhoenixBird.SIZE

        rotate(degrees = bird.rotation, pivot = Offset(cx, cy)) {
            // Trail / fire tail
            drawFireTrail(bird)

            // Body outer
            drawOval(
                color = phoenixBodyOuter,
                topLeft = Offset(bird.x, bird.y),
                size = Size(birdSize, birdSize * 0.85f)
            )

            // Body inner gradient
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(phoenixBodyInner, phoenixBodyOuter),
                    center = Offset(cx, cy),
                    radius = birdSize * 0.4f
                ),
                topLeft = Offset(bird.x + 4f, bird.y + 4f),
                size = Size(birdSize - 8f, birdSize * 0.75f)
            )

            // Belly
            drawOval(
                color = phoenixBelly,
                topLeft = Offset(cx - 6f, cy + 2f),
                size = Size(14f, 10f)
            )

            // Wing
            drawWing(bird, cx, cy)

            // Eye
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(cx + 10f, cy - 5f)
            )
            drawCircle(
                color = phoenixEye,
                radius = 3f,
                center = Offset(cx + 11f, cy - 5f)
            )

            // Beak
            val beakPath = Path().apply {
                moveTo(cx + 16f, cy - 2f)
                lineTo(cx + 24f, cy + 1f)
                lineTo(cx + 16f, cy + 4f)
                close()
            }
            drawPath(beakPath, color = phoenixBeak, style = Fill)
        }
    }

    /**
     * Draw animated wing based on current wing frame.
     */
    private fun DrawScope.drawWing(bird: PhoenixBird, cx: Float, cy: Float) {
        val wingOffset = when (bird.wingFrame) {
            0 -> -8f   // Wing up
            1 -> 0f    // Wing middle
            else -> 6f // Wing down
        }

        val wingPath = Path().apply {
            moveTo(cx - 5f, cy)
            lineTo(cx - 18f, cy + wingOffset - 8f)
            lineTo(cx - 22f, cy + wingOffset - 12f)
            lineTo(cx - 16f, cy + wingOffset)
            lineTo(cx - 5f, cy + 6f)
            close()
        }
        drawPath(wingPath, color = phoenixWing, style = Fill)

        // Wing tip highlight
        val tipPath = Path().apply {
            moveTo(cx - 16f, cy + wingOffset - 6f)
            lineTo(cx - 22f, cy + wingOffset - 12f)
            lineTo(cx - 19f, cy + wingOffset - 4f)
            close()
        }
        drawPath(tipPath, color = phoenixWingTip, style = Fill)
    }

    /**
     * Draw fire trail behind the bird.
     */
    private fun DrawScope.drawFireTrail(bird: PhoenixBird) {
        val trailColors = listOf(
            Color(0xAAFF6B35),
            Color(0x77FF8C42),
            Color(0x44FFB347),
            Color(0x22FFD700)
        )
        val cx = bird.x + PhoenixBird.SIZE / 2
        val cy = bird.y + PhoenixBird.SIZE / 2

        trailColors.forEachIndexed { index, color ->
            val trailX = cx - 12f - (index * 8f)
            val trailSize = 10f - (index * 2f)
            drawOval(
                color = color,
                topLeft = Offset(trailX - trailSize / 2, cy - trailSize / 3),
                size = Size(trailSize, trailSize * 0.6f)
            )
        }
    }
}
