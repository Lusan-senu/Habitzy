package com.htj.habitzy.ui.addedithabit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import kotlin.math.max

@Composable
fun CustomColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hsv = remember(initialColor) { toHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(hsv.first) }
    var saturation by remember { mutableFloatStateOf(hsv.second) }
    var value by remember { mutableFloatStateOf(hsv.third) }

    val currentColor = remember(hue, saturation, value) { Color.hsv(hue, saturation, value) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(HabitzyShapes.medium)
                    .background(currentColor),
            )
            Spacer(modifier = Modifier.width(SpaceM))
            Text(
                text = "Custom color",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(SpaceM))

        // Saturation / Value box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(HabitzyShapes.medium)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.hsv(hue, 1f, 1f)),
                    )
                )
                .then(
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        saturation = (pos.x / size.width).coerceIn(0f, 1f)
                        value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                        onColorChanged(Color.hsv(hue, saturation, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        onColorChanged(Color.hsv(hue, saturation, value))
                        change.consume()
                    }
                },
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val markerX = saturation * (size.width - 12.dp.toPx()) + 6.dp.toPx()
                val markerY = (1f - value) * (size.height - 12.dp.toPx()) + 6.dp.toPx()
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(markerX, markerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
                )
            }
        }

        Spacer(modifier = Modifier.height(SpaceS))

        // Hue bar
        val hueColors = listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(HabitzyShapes.small)
                .background(Brush.horizontalGradient(hueColors))
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorChanged(Color.hsv(hue, saturation, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorChanged(Color.hsv(hue, saturation, value))
                        change.consume()
                    }
                },
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val markerX = (hue / 360f) * (size.width - 12.dp.toPx()) + 6.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }

        Spacer(modifier = Modifier.height(SpaceM))
    }
}

private fun toHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxC = max(max(r, g), b)
    val minC = minOf(r, g, b)
    val delta = maxC - minC
    var hue = 0f
    if (delta != 0f) {
        hue = when (maxC) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
    }
    if (hue < 0f) hue += 360f
    val sat = if (maxC == 0f) 0f else delta / maxC
    return Triple(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), maxC.coerceIn(0f, 1f))
}
