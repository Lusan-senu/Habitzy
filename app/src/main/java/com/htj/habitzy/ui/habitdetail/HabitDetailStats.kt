package com.htj.habitzy.ui.habitdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htj.habitzy.domain.model.StreakInfo
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import java.util.Locale

@Composable
fun HabitStatsGrid(
    streakInfo: StreakInfo,
    ratePeriod: RatePeriod,
    onSelectRatePeriod: (RatePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rate = when (ratePeriod) {
        RatePeriod.D7 -> streakInfo.completionRate7d
        RatePeriod.D30 -> streakInfo.completionRate30d
        RatePeriod.D90 -> streakInfo.completionRate90d
        RatePeriod.ALL -> streakInfo.completionRateAllTime
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SpaceS),
        ) {
            StatCard(label = "Completion", value = "${(rate * 100).toInt()}%") {
                SingleChoiceSegmentedButtonRow {
                    RatePeriod.values().forEachIndexed { index, p ->
                        SegmentedButton(
                            selected = ratePeriod == p,
                            onClick = { onSelectRatePeriod(p) },
                            shape = SegmentedButtonDefaults.itemShape(index, RatePeriod.values().size),
                        ) { Text(text = p.label, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            StatCard(label = "Total", value = "${streakInfo.totalCompletions}")
            StatCard(label = "Best streak", value = "${streakInfo.bestStreak}")
            StatCard(label = "Current", value = "${streakInfo.currentStreak}")
            StatCard(
                label = "Strength",
                value = streakInfo.strengthScore?.let { "${it.toInt()}" } ?: "—",
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = HabitzyShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(SpaceM),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                trailing()
            }
        }
    }
}

@Composable
fun StreakOverTimeChart(
    weeklyCounts: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (weeklyCounts.isEmpty()) return
    val max = (weeklyCounts.maxOrNull() ?: 1).coerceAtLeast(1)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = HabitzyShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        val barColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        Canvas(modifier = Modifier.fillMaxWidth().padding(SpaceM)) {
            val barWidth = size.width / weeklyCounts.size
            weeklyCounts.forEachIndexed { index, count ->
                val barHeight = (count.toFloat() / max) * (size.height - 8.dp.toPx())
                val barSlot = Size(Math.max(barWidth * 0.6f, 2.dp.toPx()), barHeight)
                val left = index * barWidth + (barWidth - barSlot.width) / 2
                val top = size.height - barHeight
                drawRoundRect(
                    color = trackColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barSlot.height),
                    size = barSlot.copy(height = barSlot.height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawRoundRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = barSlot,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}
