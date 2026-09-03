package com.htj.habitzy.ui.habits

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.htj.habitzy.ui.theme.SpaceXXL
import kotlin.random.Random

@Composable
fun CelebrationConfetti(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (visible) {
            val palette = listOf(
                com.htj.habitzy.ui.theme.HabitSwatch1,
                com.htj.habitzy.ui.theme.HabitSwatch2,
                com.htj.habitzy.ui.theme.HabitSwatch5,
                com.htj.habitzy.ui.theme.HabitSwatch6,
                com.htj.habitzy.ui.theme.HabitSwatch8,
                com.htj.habitzy.ui.theme.HabitSwatch11,
            )
            val pieces = rememberPieces(count = 40, palette = palette)
            val transition = rememberInfiniteTransition(label = "confetti")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "confetti_progress",
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                pieces.forEach { piece ->
                    val y = piece.startY * (1f - progress) + piece.endY * progress
                    val x = piece.startX * (1f - progress) + piece.endX * progress
                    val sway = kotlin.math.sin((progress * 6.283f) + piece.phase) * piece.swayAmplitude
                    drawCircle(
                        color = piece.color,
                        radius = piece.radius,
                        center = Offset(x + sway, y),
                    )
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val color: Color,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val swayAmplitude: Float,
    val phase: Float,
    val radius: Float,
)

@Composable
private fun rememberPieces(count: Int, palette: List<Color>): List<ConfettiPiece> {
    val random = remember { Random(42) }
    return remember(palette) {
        List(count) {
            ConfettiPiece(
                color = palette[random.nextInt(palette.size)],
                startX = 0.4f * SpaceXXL.value,
                startY = 0.4f,
                endX = random.nextFloat(),
                endY = random.nextFloat() * 0.9f + 0.1f,
                swayAmplitude = random.nextFloat() * 80f,
                phase = random.nextFloat() * 6.283f,
                radius = random.nextFloat() * 6f + 3f,
            )
        }
    }
}
