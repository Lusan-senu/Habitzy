package com.htj.habitzy.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.htj.habitzy.domain.model.HabitRankEntry
import com.htj.habitzy.domain.usecase.InsightPeriod
import com.htj.habitzy.ui.components.HabitIconChip
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.components.StreakBadge
import com.htj.habitzy.ui.theme.DisplayMediumEmphasized
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXL
import com.htj.habitzy.ui.theme.SpaceXS
import com.htj.habitzy.ui.theme.SpaceXXS
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("Insights") },
                colors = TopAppBarDefaults.largeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            PeriodSelector(
                selected = uiState.period,
                onSelect = viewModel::selectPeriod,
            )

            val summary = uiState.summary
            if (!uiState.hasInsightData || summary == null) {
                InsufficientDataHint(
                    modifier = Modifier.padding(SpaceXL),
                )
            } else {
                OverviewCard(
                    rate = uiState.overallRateForPeriod,
                    period = uiState.period,
                    modifier = Modifier.padding(horizontal = SpaceL),
                )

                HabitPerformanceSection(
                    entries = summary.perHabitRanking,
                    modifier = Modifier.padding(top = SpaceXL),
                )

                BestStreaksSection(
                    entries = summary.bestCurrentStreaks,
                    modifier = Modifier.padding(top = SpaceXL),
                )

                ConsistencyByHourSection(
                    histogram = summary.hourOfDayHistogram,
                    modifier = Modifier.padding(top = SpaceXL),
                )

                MonthlyHeatmapSection(
                    entries = summary.perHabitRanking,
                    focusedHabitId = uiState.focusedHabitId,
                    onHabitSelect = viewModel::selectHabit,
                    heatmap = summary.monthlyHeatmap,
                    modifier = Modifier.padding(top = SpaceXL),
                )

                MomentumSection(
                    trend = summary.weeklyCompletionsTrend,
                    modifier = Modifier.padding(top = SpaceXL, bottom = SpaceXL),
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: InsightPeriod,
    onSelect: (InsightPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        InsightPeriod.TODAY to "Today",
        InsightPeriod.WEEK to "Week",
        InsightPeriod.MONTH to "Month",
        InsightPeriod.ALL_TIME to "All time",
    )
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpaceL),
    ) {
        options.forEachIndexed { index, (period, label) ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun OverviewCard(
    rate: Float,
    period: InsightPeriod,
    modifier: Modifier = Modifier,
) {
    val periodLabel = when (period) {
        InsightPeriod.TODAY -> "of habits completed today"
        InsightPeriod.WEEK -> "of scheduled habits completed this week"
        InsightPeriod.MONTH -> "of scheduled habits completed this month"
        InsightPeriod.ALL_TIME -> "of scheduled habits ever completed"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HabitzyShapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${(rate * 100).roundToInt()}%",
                style = DisplayMediumEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SpaceS))
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HabitPerformanceSection(
    entries: List<HabitRankEntry>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "Habit performance")
        val shown = entries.take(8)
        shown.forEach { entry ->
            HabitRankRow(entry = entry, modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS))
        }
        if (entries.size > 8) {
            Text(
                text = "…and ${entries.size - 8} more habits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceS),
            )
        }
    }
}

@Composable
private fun HabitRankRow(
    entry: HabitRankEntry,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitIconChip(icon = entry.icon, color = Color(entry.color), size = 32)
            Spacer(Modifier.width(SpaceM))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(entry.value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(SpaceXS))
        LinearProgressIndicator(
            progress = { entry.value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(entry.color),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun BestStreaksSection(
    entries: List<HabitRankEntry>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "On a roll 🔥")
        if (entries.isEmpty()) {
            Text(
                text = "No streaks yet — start building momentum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SpaceL),
            )
            return
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = SpaceL),
            horizontalArrangement = Arrangement.spacedBy(SpaceM),
        ) {
            items(entries, key = { it.habitId }) { entry ->
                Surface(
                    shape = HabitzyShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(SpaceL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HabitIconChip(icon = entry.icon, color = Color(entry.color), size = 40)
                        Spacer(Modifier.height(SpaceM))
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 96.dp),
                        )
                        Spacer(Modifier.height(SpaceS))
                        StreakBadge(streak = entry.value.toInt())
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsistencyByHourSection(
    histogram: IntArray,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "When are you most consistent?")
        val max = histogram.maxOrNull()?.coerceAtLeast(1) ?: 1
        BarChart(
            values = histogram.toList(),
            maxValue = max.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceL)
                .height(120.dp),
        )
        Spacer(Modifier.height(SpaceXS))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("12 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("6 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("12 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("6 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("11 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthlyHeatmapSection(
    entries: List<HabitRankEntry>,
    focusedHabitId: Long?,
    onHabitSelect: (Long?) -> Unit,
    heatmap: List<Float>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "This month at a glance")
        if (entries.isNotEmpty()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = SpaceL),
                horizontalArrangement = Arrangement.spacedBy(SpaceS),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = focusedHabitId == null,
                        onClick = { onHabitSelect(null) },
                        label = { Text("All habits") },
                    )
                }
                items(entries.sortedBy { it.name.lowercase() }, key = { it.habitId }) { entry ->
                    FilterChip(
                        selected = focusedHabitId == entry.habitId,
                        onClick = { onHabitSelect(if (focusedHabitId == entry.habitId) null else entry.habitId) },
                        label = { Text(entry.name) },
                    )
                }
            }
            Spacer(Modifier.height(SpaceL))
            HeatmapGrid(
                heatmap = heatmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpaceL),
            )
        } else {
            Text(
                text = "No habits yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SpaceL),
            )
        }
        Spacer(Modifier.height(SpaceL))
    }
}

@Composable
private fun HeatmapGrid(
    heatmap: List<Float>,
    modifier: Modifier = Modifier,
) {
    if (heatmap.isEmpty()) return

    val today = java.time.LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val firstWeekday = today.withDayOfMonth(1).dayOfWeek.value

    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(SpaceS))

        val leadingBlanks = (firstWeekday - 1)
        val totalCells = leadingBlanks + daysInMonth
        val weeks = (totalCells + 6) / 7

        // Track the real calendar day as we walk the flattened cells (blanks first).
        var dayOfMonth = 0
        weeksCells(leadingBlanks, daysInMonth, heatmap).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val isBlank = cell == null
                    if (!isBlank) dayOfMonth++
                    val cellDate = if (isBlank) null else today.withDayOfMonth(dayOfMonth)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(SpaceXXS)
                            .then(
                                if (cellDate != null) {
                                    Modifier.semantics {
                                        contentDescription = heatmapCellContentDescription(
                                            date = cellDate,
                                            rate = cell!!.coerceIn(0f, 1f),
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null) {
                            val rate = cell.coerceIn(0f, 1f)
                            val bg = heatColor(rate)
                            val onColor = if (rate > 0.55f) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(bg),
                            )
                            Text(
                                text = cellLabel(rate),
                                style = MaterialTheme.typography.labelSmall,
                                color = onColor,
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(SpaceS))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            (0..4).forEach { level ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .padding(SpaceXXS)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .background(heatColor(level / 4f)),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(SpaceXS))
        Text(
            text = "Shade = completion rate for the day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun cellLabel(rate: Float): String = when {
    rate >= 0.999f -> "✓"
    rate >= 0.5f -> "·"
    else -> if (rate <= 0f) "–" else "·"
}

/** TalkBack label for a combined-monthly-heatmap cell (§13.2). */
private fun heatmapCellContentDescription(date: java.time.LocalDate, rate: Float): String {
    val formatted = date.format(
        java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.getDefault()),
    )
    return when {
        rate >= 0.999f -> "$formatted — all scheduled habits completed"
        rate > 0f -> "$formatted — ${(rate * 100).toInt()} percent of scheduled habits completed"
        else -> "$formatted — none completed"
    }
}

private fun weeksCells(
    leadingBlanks: Int,
    daysInMonth: Int,
    heatmap: List<Float>,
): List<List<Float?>> {
    val cells = MutableList<Float?>(leadingBlanks) { null }
    cells.addAll(heatmap.take(daysInMonth))
    val result = mutableListOf<List<Float?>>()
    for (i in cells.indices step 7) {
        val week = cells.subList(i, (i + 7).coerceAtMost(cells.size)).toMutableList()
        while (week.size < 7) week.add(null)
        result.add(week)
    }
    return result
}

@Composable
private fun heatColor(rate: Float): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        rate <= 0f -> scheme.surfaceContainerHighest
        rate < 0.34f -> scheme.primary.copy(alpha = 0.25f)
        rate < 0.67f -> scheme.primary.copy(alpha = 0.5f)
        else -> scheme.primary
    }
}

@Composable
private fun MomentumSection(
    trend: List<Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "Momentum")
        if (trend.isEmpty()) {
            Text(
                text = "Not enough data yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SpaceL),
            )
            return
        }
        val max = (trend.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        LineChart(
            values = trend,
            maxValue = max,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceL)
                .height(140.dp),
        )
        Spacer(Modifier.height(SpaceXS))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("12 wk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("6 wk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsufficientDataHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HabitzyShapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Keep logging",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(SpaceS))
            Text(
                text = "Insights unlock after a few days of data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BarChart(
    values: List<Int>,
    maxValue: Float,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (values.isEmpty()) return@Canvas
        val barGap = SpaceXXS.toPx()
        val barWidth = (w - barGap * (values.size - 1)) / values.size
        values.forEachIndexed { index, value ->
            val left = index * (barWidth + barGap)
            val fraction = if (maxValue > 0) value / maxValue else 0f
            val barHeight = h * fraction.coerceIn(0f, 1f)
            val color = if (value > 0) barColor else trackColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, h - barHeight.coerceAtLeast(1f)),
                size = Size(barWidth, if (barHeight >= 1f) barHeight else 1f),
                cornerRadius = CornerRadius(SpaceXXS.toPx()),
            )
        }
    }
}

@Composable
private fun LineChart(
    values: List<Int>,
    maxValue: Float,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val points = remember(values) {
        if (values.size < 2) return@remember emptyList()
        val n = values.size
        (0 until n).map { i ->
            val xFraction = if (n > 1) i / (n - 1).toFloat() else 0f
            val yFraction = if (maxValue > 0) values[i] / maxValue else 0f
            Pair(xFraction, yFraction)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 4.dp.toPx()
        if (points.isEmpty()) return@Canvas
        val path = Path()
        points.forEachIndexed { index, (xf, yf) ->
            val x = pad + xf * (w - pad * 2)
            val y = h - pad - yf * (h - pad * 2)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
        points.forEach { (xf, yf) ->
            val x = pad + xf * (w - pad * 2)
            val y = h - pad - yf * (h - pad * 2)
            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}
