package com.htj.habitzy.ui.habits

import com.htj.habitzy.ui.components.HabitWithTodayLog
import java.time.DayOfWeek
import java.time.LocalDate

enum class SortMode { MANUAL, NAME, CREATED_DATE, CURRENT_STREAK }

data class HabitsUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDates: List<LocalDate> = emptyList(),
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val habits: List<HabitWithTodayLog> = emptyList(),
    val groupedHabits: List<HabitGroup> = emptyList(),
    val showArchived: Boolean = false,
    val activeCategoryFilters: Set<String> = emptySet(),
    val availableCategoryTags: List<String> = emptyList(),
    val sortMode: SortMode = SortMode.MANUAL,
    val hasAnyHabits: Boolean = false,
)

data class HabitGroup(
    val categoryTag: String?,
    val habits: List<HabitWithTodayLog>,
)

data class HabitsSnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val action: HabitsSnackbarAction? = null,
)

sealed class HabitsSnackbarAction {
    data class UndoToggle(val habitId: Long, val date: LocalDate) : HabitsSnackbarAction()
}
