package com.htj.habitzy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.ui.theme.HabitzyElevation
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceS

data class HabitWithTodayLog(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val amountProgress: Double = 0.0,
    val checklistDoneCount: Int = 0,
    val currentStreak: Int = 0,
)

@Composable
fun HabitCard(
    habitWithLog: HabitWithTodayLog,
    onBodyClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeArchive: (() -> Unit)? = null,
    onSwipeEdit: (() -> Unit)? = null,
) {
    val habit = habitWithLog.habit
    val habitColor = Color(habit.color)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HabitzyShapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = HabitzyElevation.Level1.shadowDp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onBodyClick)
                .padding(SpaceL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitIconChip(icon = habit.icon, color = habitColor)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SpaceM),
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = habit.schedule.summaryText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (habitWithLog.currentStreak > 0) {
                StreakBadge(streak = habitWithLog.currentStreak)
            }
            HabitCompletionControl(
                habit = habit,
                isCompleted = habitWithLog.isCompletedToday,
                amountProgress = habitWithLog.amountProgress,
                checklistDoneCount = habitWithLog.checklistDoneCount,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
fun HabitIconChip(
    icon: HabitIcon,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    val containerColor = color.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(ShapeFull)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        when (icon) {
            is HabitIcon.Emoji -> {
                Text(
                    text = icon.char,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            is HabitIcon.Bundled -> {
                Text(
                    text = icon.key.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun HabitCompletionControl(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    amountProgress: Double = 0.0,
    checklistDoneCount: Int = 0,
) {
    when (habit.type) {
        is HabitType.Binary -> {
            val scale by animateFloatAsState(
                targetValue = if (isCompleted) 1f else 0.8f,
                label = "check_scale",
            )
            val bgColor by animateColorAsState(
                targetValue = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                label = "check_bg",
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        is HabitType.Amount -> {
            val goal = (habit.type as HabitType.Amount).goal
            val fraction = if (goal > 0) (amountProgress / goal).coerceIn(0.0, 1.0) else 0.0
            ProgressRing(
                progress = fraction.toFloat(),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onToggle),
                size = 36.dp,
                color = Color(habit.color),
            )
        }
        is HabitType.Checklist -> {
            val total = (habit.type as HabitType.Checklist).steps.size
            Surface(
                modifier = Modifier.clickable(onClick = onToggle),
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "$checklistDoneCount/$total",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = SpaceS, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
