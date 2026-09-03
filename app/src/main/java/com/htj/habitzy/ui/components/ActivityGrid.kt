package com.htj.habitzy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ActivityGrid(
    logs: Map<LocalDate, Float>,
    modifier: Modifier = Modifier,
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    onDayClick: ((LocalDate) -> Unit)? = null,
    onDayLongPress: ((LocalDate) -> Unit)? = null,
) {
    val today = LocalDate.now()
    val weeks = remember(weekStartDay) {
        val endDate = today
        val startDate = endDate.minusDays(364)
        val startAdjustment = (weekStartDay.value - startDate.dayOfWeek.value + 7) % 7
        val adjustedStart = startDate.plusDays(startAdjustment.toLong())

        val allWeeks = mutableListOf<List<LocalDate>>()
        var current = adjustedStart
        while (current <= endDate) {
            val week = (0..6).map { current.plusDays(it.toLong()) }
            allWeeks.add(week)
            current = current.plusDays(7)
        }
        allWeeks
    }

    val dayLabels = remember(weekStartDay) {
        val days = mutableListOf<DayOfWeek>()
        var d = weekStartDay
        repeat(7) {
            days.add(d)
            d = if (d == DayOfWeek.SUNDAY) DayOfWeek.MONDAY else DayOfWeek.of(d.value + 1)
        }
        days.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = SpaceXS),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    week.forEach { date ->
                        val intensity = when {
                            date > today -> 0f
                            logs.containsKey(date) -> logs[date] ?: 0f
                            else -> 0f
                        }
                        val isFuture = date > today
                        val cellColor = if (isFuture) {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        } else {
                            intensityToColor(intensity)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(HabitzyShapes.extraSmall)
                                .background(cellColor)
                                .semantics {
                                    contentDescription = dayContentDescription(
                                        date = date,
                                        intensity = intensity,
                                        isFuture = isFuture,
                                    )
                                }
                                .then(
                                    if (!isFuture && onDayClick != null) {
                                        Modifier.clickable { onDayClick(date) }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (date == today) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(ShapeFull)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun intensityToColor(intensity: Float): Color {
    if (intensity <= 0f) return MaterialTheme.colorScheme.surfaceContainerLowest
    val primary = MaterialTheme.colorScheme.primary
    val containerLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    return lerpColor(containerLowest, primary, intensity.coerceIn(0f, 1f))
}

/** TalkBack label for a color-coded grid cell (§13.2): date + completion state. */
private fun dayContentDescription(
    date: LocalDate,
    intensity: Float,
    isFuture: Boolean,
): String {
    val formatted = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    return when {
        isFuture -> "$formatted — upcoming"
        intensity >= 0.999f -> "$formatted — completed"
        intensity > 0f -> "$formatted — ${(intensity * 100).toInt()} percent complete"
        else -> "$formatted — not completed"
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val red = from.red + (to.red - from.red) * fraction
    val green = from.green + (to.green - from.green) * fraction
    val blue = from.blue + (to.blue - from.blue) * fraction
    val alpha = from.alpha + (to.alpha - from.alpha) * fraction
    return Color(red, green, blue, alpha)
}
