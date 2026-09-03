package com.htj.habitzy.ui.habitdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.components.ActivityGrid
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXXS
import com.htj.habitzy.ui.theme.SpaceXS
import com.htj.habitzy.ui.theme.SpaceXXS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DetailRangeGrid(
    range: DetailRange,
    logs: Map<LocalDate, Boolean>,
    vacationRange: ClosedRange<LocalDate>?,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (range) {
        DetailRange.WEEK -> WeekGrid(
            logs = logs,
            vacationRange = vacationRange,
            weekStartDay = weekStartDay,
            onDayClick = onDayClick,
            onDayLongPress = onDayLongPress,
            modifier = modifier,
        )
        DetailRange.MONTH -> MonthGrid(
            logs = logs,
            vacationRange = vacationRange,
            weekStartDay = weekStartDay,
            onDayClick = onDayClick,
            onDayLongPress = onDayLongPress,
            modifier = modifier,
        )
        DetailRange.YEAR -> {
            val intensity = logs.mapValues { if (it.value) 1f else 0f }
            ActivityGrid(
                logs = intensity,
                modifier = modifier,
                weekStartDay = weekStartDay,
                onDayClick = onDayClick,
                onDayLongPress = onDayLongPress,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeekGrid(
    logs: Map<LocalDate, Boolean>,
    vacationRange: ClosedRange<LocalDate>?,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val start = today.minusDays(((today.dayOfWeek.value - weekStartDay.value + 7) % 7).toLong())

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpaceXXS),
        ) {
            (0..6).forEach { offset ->
                val date = start.plusDays(offset.toLong())
                val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SpaceXS),
            horizontalArrangement = Arrangement.spacedBy(SpaceXXS),
        ) {
            (0..6).forEach { offset ->
                val date = start.plusDays(offset.toLong())
                DayCell(
                    date = date,
                    isCompleted = logs[date] ?: false,
                    isVacation = vacationRange?.let { date in it } == true,
                    onDayClick = onDayClick,
                    onDayLongPress = onDayLongPress,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthGrid(
    logs: Map<LocalDate, Boolean>,
    vacationRange: ClosedRange<LocalDate>?,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val firstOfMonth = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val start = firstOfMonth.minusDays(((firstOfMonth.dayOfWeek.value - weekStartDay.value + 7) % 7).toLong())

    val weeks = rememberWeeks(start, daysInMonth, firstOfMonth)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpaceXXS),
        ) {
            (0..6).forEach { offset ->
                val d = weekStartDay.plus(offset.toLong())
                Text(
                    text = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = SpaceXS),
            verticalArrangement = Arrangement.spacedBy(SpaceXXS),
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpaceXXS),
                ) {
                    week.forEach { date ->
                        if (date.month == today.month) {
                            DayCell(
                                date = date,
                                isCompleted = logs[date] ?: false,
                                isVacation = vacationRange?.let { date in it } == true,
                                onDayClick = onDayClick,
                                onDayLongPress = onDayLongPress,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberWeeks(
    start: LocalDate,
    daysInMonth: Int,
    firstOfMonth: LocalDate,
): List<List<LocalDate>> {
    val result = mutableListOf<List<LocalDate>>()
    var current = start
    while (current <= firstOfMonth.plusDays(daysInMonth.toLong() - 1)) {
        result.add((0..6).map { current.plusDays(it.toLong()) })
        current = current.plusWeeks(1)
    }
    return result
}

/** TalkBack label for a detail-grid day cell (§13.2), including the vacation state. */
private fun detailDayContentDescription(
    date: LocalDate,
    isCompleted: Boolean,
    isVacation: Boolean,
    isFuture: Boolean,
): String {
    val formatted = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    return when {
        isFuture -> "$formatted — upcoming"
        isVacation -> "$formatted — vacation, not counted"
        isCompleted -> "$formatted — completed"
        else -> "$formatted — not completed"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    isCompleted: Boolean,
    isVacation: Boolean,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val isToday = date == today
    val isFuture = date > today
    val color = when {
        isVacation -> MaterialTheme.colorScheme.surfaceContainerLow
        isFuture -> MaterialTheme.colorScheme.surfaceContainerLowest
        isCompleted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(HabitzyShapes.extraSmall)
            .background(color)
            .semantics {
                contentDescription = detailDayContentDescription(
                    date = date,
                    isCompleted = isCompleted,
                    isVacation = isVacation,
                    isFuture = isFuture,
                )
            }
            .combinedClickable(
                onClick = { if (!isFuture) onDayClick(date) },
                onLongClick = { onDayLongPress(date) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
        )
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(4.dp)
                    .clip(ShapeFull)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
