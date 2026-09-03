package com.htj.habitzy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring

/**
 * Wraps a [HabitCard] with a horizontal swipe to reveal Edit / Archive actions.
 */
@Composable
fun SwipeRevealHabitCard(
    habitWithLog: HabitWithTodayLog,
    onBodyClick: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val actionWidth = 168.dp
    var offsetX by remember { mutableFloatStateOf(0f) }

    fun settleTo(target: Float) {
        scope.launch {
            animate(
                initialValue = offsetX,
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) { value, _ -> offsetX = value }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val over = offsetX <= (-actionWidth.toPx() / 2f)
                        settleTo(if (over) -actionWidth.toPx() else 0f)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val max = -actionWidth.toPx()
                        offsetX = (offsetX + dragAmount).coerceIn(max, 0f)
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .zIndex(0f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                TextButton(onClick = { offsetX = 0f; onEdit() }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Edit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.errorContainer),
            ) {
                TextButton(onClick = { offsetX = 0f; onArchive() }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Archive", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        HabitCard(
            habitWithLog = habitWithLog,
            onBodyClick = onBodyClick,
            onToggle = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
        )
    }
}
