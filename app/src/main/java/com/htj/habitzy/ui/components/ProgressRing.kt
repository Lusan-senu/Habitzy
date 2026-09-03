package com.htj.habitzy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.SpaceXS

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        label = "progress_ring",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = stroke,
        )
    }
}
