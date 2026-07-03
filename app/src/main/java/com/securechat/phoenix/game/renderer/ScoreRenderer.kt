package com.securechat.phoenix.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Renders score-related text overlays on the game canvas.
 */
object ScoreRenderer {

    private val scoreStyle = TextStyle(
        color = Color.White,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color(0x88000000),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )

    /**
     * Draw the current score during gameplay.
     */
    fun drawScore(drawScope: DrawScope, textMeasurer: TextMeasurer, score: Int) {
        val text = score.toString()
        val measured = textMeasurer.measure(text, scoreStyle)

        with(drawScope) {
            val x = (size.width - measured.size.width) / 2f
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x, 60f)
            )
        }
    }

    /**
     * Draw the "Tap to Start" text on the ready screen.
     */
    fun drawReadyOverlay(drawScope: DrawScope, textMeasurer: TextMeasurer) {
        val titleStyle = TextStyle(
            color = Color(0xFFFF6B35),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color(0x88000000),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )
        val subtitleStyle = TextStyle(
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        with(drawScope) {
            val title = textMeasurer.measure("Flying Phoenix", titleStyle)
            val subtitle = textMeasurer.measure("Tap to Fly!", subtitleStyle)

            drawText(
                textLayoutResult = title,
                topLeft = Offset(
                    (size.width - title.size.width) / 2f,
                    size.height * 0.25f
                )
            )
            drawText(
                textLayoutResult = subtitle,
                topLeft = Offset(
                    (size.width - subtitle.size.width) / 2f,
                    size.height * 0.25f + title.size.height + 16f
                )
            )
        }
    }

    /**
     * Draw the death/game over overlay.
     */
    fun drawDeathOverlay(
        drawScope: DrawScope,
        textMeasurer: TextMeasurer,
        score: Int,
        highScore: Int,
        isNewHighScore: Boolean
    ) {
        val gameOverStyle = TextStyle(
            color = Color(0xFFFF4500),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color(0x88000000),
                offset = Offset(2f, 2f),
                blurRadius = 6f
            )
        )
        val scoreTextStyle = TextStyle(
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        val highScoreStyle = TextStyle(
            color = Color(0xFFFFD700),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
        val newRecordStyle = TextStyle(
            color = Color(0xFFFF6B35),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        val restartStyle = TextStyle(
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        with(drawScope) {
            // Dim overlay
            drawRect(color = Color(0x88000000))

            val centerX = size.width / 2f
            var yPos = size.height * 0.25f

            // Game Over
            val gameOver = textMeasurer.measure("Game Over", gameOverStyle)
            drawText(gameOver, topLeft = Offset(centerX - gameOver.size.width / 2f, yPos))
            yPos += gameOver.size.height + 32f

            // Score
            val scoreText = textMeasurer.measure("Score: $score", scoreTextStyle)
            drawText(scoreText, topLeft = Offset(centerX - scoreText.size.width / 2f, yPos))
            yPos += scoreText.size.height + 16f

            // High Score
            val highScoreText = textMeasurer.measure("Best: $highScore", highScoreStyle)
            drawText(highScoreText, topLeft = Offset(centerX - highScoreText.size.width / 2f, yPos))
            yPos += highScoreText.size.height + 16f

            // New record indicator
            if (isNewHighScore) {
                val newRecord = textMeasurer.measure("NEW RECORD!", newRecordStyle)
                drawText(newRecord, topLeft = Offset(centerX - newRecord.size.width / 2f, yPos))
                yPos += newRecord.size.height + 16f
            }

            yPos += 32f

            // Restart prompt
            val restart = textMeasurer.measure("Tap to Restart", restartStyle)
            drawText(restart, topLeft = Offset(centerX - restart.size.width / 2f, yPos))
        }
    }
}
