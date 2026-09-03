package com.htj.habitzy.ui.habitdetail

import com.htj.habitzy.domain.model.DayNote
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.StreakInfo
import java.time.LocalDate

enum class DetailRange { WEEK, MONTH, YEAR }

enum class RatePeriod(val label: String) {
    D7("7d"),
    D30("30d"),
    D90("90d"),
    ALL("All"),
}

data class HabitDetailUiState(
    val isLoading: Boolean = true,
    val habit: Habit? = null,
    val streakInfo: StreakInfo = StreakInfo(0, 0, 0, 0f, 0f, 0f, 0f, null),
    val completionLogs: Map<LocalDate, HabitLog> = emptyMap(),
    val notes: List<DayNote> = emptyList(),
    val range: DetailRange = DetailRange.MONTH,
    val ratePeriod: RatePeriod = RatePeriod.D30,
    val weeklyCompletionCounts: List<Int> = emptyList(),
)

sealed class DetailSnackbarAction

data class DetailSnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val action: DetailSnackbarAction? = null,
) {
    object Archive : DetailSnackbarAction()
    object Unarchive : DetailSnackbarAction()
}
