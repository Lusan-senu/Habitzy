package com.htj.habitzy.ui.insights

import com.htj.habitzy.domain.model.HabitRankEntry
import com.htj.habitzy.domain.model.InsightSummary
import com.htj.habitzy.domain.usecase.InsightPeriod
import java.time.DayOfWeek

data class InsightsUiState(
    val isLoading: Boolean = true,
    val period: InsightPeriod = InsightPeriod.WEEK,
    val summary: InsightSummary? = null,
    val focusedHabitId: Long? = null,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
) {
    val habitFilterOptions: List<HabitRankEntry>
        get() = summary?.let { s ->
            s.perHabitRanking.sortedBy { it.name.lowercase() }
        } ?: emptyList()

    val hasInsightData: Boolean
        get() = summary?.distinctLoggedDays ?: 0 >= 3

    val overallRateForPeriod: Float
        get() {
            val s = summary ?: return 0f
            return when (period) {
                InsightPeriod.TODAY -> s.overallCompletionRateToday
                InsightPeriod.WEEK -> s.overallCompletionRateWeek
                InsightPeriod.MONTH -> s.overallCompletionRateMonth
                InsightPeriod.ALL_TIME -> s.overallCompletionRateAllTime
            }
        }
}
