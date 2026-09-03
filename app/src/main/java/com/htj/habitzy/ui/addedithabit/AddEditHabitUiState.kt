package com.htj.habitzy.ui.addedithabit

import androidx.compose.ui.graphics.toArgb
import com.htj.habitzy.ui.theme.HabitSwatch1

enum class FormHabitType { BINARY, AMOUNT, CHECKLIST }

enum class ScheduleKind { DAILY, SPECIFIC_WEEKDAYS, TIMES_PER_WEEK, TIMES_PER_MONTH, EVERY_N_DAYS }

data class ReminderDraft(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val message: String,
    val enabled: Boolean,
)

data class AddEditHabitUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val name: String = "",
    val emoji: String? = null,
    val colorArgb: Int = HabitSwatch1.toArgb(),
    val type: FormHabitType = FormHabitType.BINARY,
    val amountGoalText: String = "",
    val amountUnit: String = "",
    val checklistSteps: List<String> = listOf("", ""),
    val scheduleKind: ScheduleKind = ScheduleKind.DAILY,
    val selectedWeekdays: Set<Int> = emptySet(),
    val timesPerWeekText: String = "3",
    val timesPerMonthText: String = "10",
    val everyNDaysText: String = "2",
    val reminders: List<ReminderDraft> = emptyList(),
    val categoryTag: String = "",
    val description: String = "",
    val usedCategoryTags: List<String> = emptyList(),
) {
    val canSave: Boolean get() {
        if (name.isBlank()) return false
        return when (type) {
            FormHabitType.BINARY -> true
            FormHabitType.AMOUNT -> (amountGoalText.toDoubleOrNull() ?: 0.0) > 0.0
            FormHabitType.CHECKLIST -> checklistSteps.count { it.isNotBlank() } >= 2
        } && when (scheduleKind) {
            ScheduleKind.DAILY -> true
            ScheduleKind.SPECIFIC_WEEKDAYS -> selectedWeekdays.isNotEmpty()
            ScheduleKind.TIMES_PER_WEEK -> (timesPerWeekText.toIntOrNull() ?: 0) > 0
            ScheduleKind.TIMES_PER_MONTH -> (timesPerMonthText.toIntOrNull() ?: 0) > 0
            ScheduleKind.EVERY_N_DAYS -> (everyNDaysText.toIntOrNull() ?: 0) >= 1
        }
    }
}
