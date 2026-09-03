package com.htj.habitzy.ui.addedithabit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.domain.model.Reminder
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import com.htj.habitzy.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val notificationScheduler: NotificationScheduler,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val habitId: Long? = savedStateHandle.get<Long>("habitId")

    private val _uiState = MutableStateFlow(AddEditHabitUiState())
    val uiState: StateFlow<AddEditHabitUiState> = _uiState.asStateFlow()

    private var existingHabit: Habit? = null

    private var defaultReminderHour = 9
    private var defaultReminderMinute = 0

    private val _saved = Channel<Boolean>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            defaultReminderHour = settingsRepository.defaultReminderHour.first()
            defaultReminderMinute = settingsRepository.defaultReminderMinute.first()
            val tags = habitRepository.getUsedCategoryTags()
            if (habitId == null) {
                _uiState.value = AddEditHabitUiState(isLoading = false, usedCategoryTags = tags)
            }
        }
        if (habitId != null) {
            viewModelScope.launch {
                val habit = habitRepository.observeHabit(habitId).first() ?: return@launch
                val reminders = habitRepository.observeReminders(habitId).first()
                val tags = habitRepository.getUsedCategoryTags()
                existingHabit = habit
                _uiState.value = prefill(habit, reminders, tags)
            }
        }
    }

    private fun prefill(habit: Habit, reminders: List<Reminder>, tags: List<String>): AddEditHabitUiState {
        val type = when (val t = habit.type) {
            is HabitType.Binary -> FormHabitType.BINARY
            is HabitType.Amount -> FormHabitType.AMOUNT
            is HabitType.Checklist -> FormHabitType.CHECKLIST
        }
        val schedule = when (val s = habit.schedule) {
            is HabitSchedule.Daily -> ScheduleKind.DAILY
            is HabitSchedule.SpecificWeekdays -> ScheduleKind.SPECIFIC_WEEKDAYS
            is HabitSchedule.TimesPerWeek -> ScheduleKind.TIMES_PER_WEEK
            is HabitSchedule.TimesPerMonth -> ScheduleKind.TIMES_PER_MONTH
            is HabitSchedule.EveryNDays -> ScheduleKind.EVERY_N_DAYS
        }
        return AddEditHabitUiState(
            isEditing = true,
            isLoading = false,
            name = habit.name,
            emoji = (habit.icon as? HabitIcon.Emoji)?.char,
            colorArgb = habit.color,
            type = type,
            amountGoalText = (habit.type as? HabitType.Amount)?.goal?.let { fmtNumber(it) } ?: "",
            amountUnit = (habit.type as? HabitType.Amount)?.unit ?: "",
            checklistSteps = (habit.type as? HabitType.Checklist)?.steps?.ifEmpty { listOf("", "") }
                ?: listOf("", ""),
            scheduleKind = schedule,
            selectedWeekdays = (habit.schedule as? HabitSchedule.SpecificWeekdays)
                ?.weekdays?.map { it.value }?.toSet() ?: emptySet(),
            timesPerWeekText = (habit.schedule as? HabitSchedule.TimesPerWeek)?.target?.toString() ?: "3",
            timesPerMonthText = (habit.schedule as? HabitSchedule.TimesPerMonth)?.target?.toString() ?: "10",
            everyNDaysText = (habit.schedule as? HabitSchedule.EveryNDays)?.n?.toString() ?: "2",
            reminders = reminders.map { ReminderDraft(it.id, it.hour, it.minute, it.message ?: "", it.isEnabled) },
            categoryTag = habit.categoryTag ?: "",
            description = habit.description ?: "",
            usedCategoryTags = tags,
        )
    }

    private fun fmtNumber(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    // --- Field mutations ---

    fun onNameChange(value: String) = update { it.copy(name = value) }
    fun onEmojiChange(value: String) = update { it.copy(emoji = value) }
    fun onColorChange(colorArgb: Int) = update { it.copy(colorArgb = colorArgb) }
    fun onTypeChange(type: FormHabitType) = update { it.copy(type = type) }
    fun onAmountGoalChange(value: String) = update { it.copy(amountGoalText = value) }
    fun onAmountUnitChange(value: String) = update { it.copy(amountUnit = value) }
    fun onChecklistStepChange(index: Int, value: String) = update {
        it.copy(checklistSteps = it.checklistSteps.toMutableList().also { l -> l[index] = value })
    }
    fun addChecklistStep() = update {
        if (it.checklistSteps.size >= 8) it else it.copy(checklistSteps = it.checklistSteps + "")
    }
    fun removeChecklistStep(index: Int) = update {
        val list = it.checklistSteps.toMutableList()
        if (list.size > 2) list.removeAt(index)
        it.copy(checklistSteps = list)
    }
    fun moveChecklistStep(from: Int, to: Int) = update {
        val list = it.checklistSteps.toMutableList()
        if (to in list.indices) list.add(to, list.removeAt(from))
        it.copy(checklistSteps = list)
    }
    fun onScheduleKindChange(kind: ScheduleKind) = update { it.copy(scheduleKind = kind) }
    fun toggleWeekday(dayValue: Int) = update {
        val current = it.selectedWeekdays
        val next = if (dayValue in current) current - dayValue else current + dayValue
        it.copy(selectedWeekdays = next)
    }
    fun onTimesPerWeekChange(value: String) = update { it.copy(timesPerWeekText = value) }
    fun onTimesPerMonthChange(value: String) = update { it.copy(timesPerMonthText = value) }
    fun onEveryNDaysChange(value: String) = update { it.copy(everyNDaysText = value) }
    fun onCategoryTagChange(value: String) = update { it.copy(categoryTag = value) }
    fun onDescriptionChange(value: String) = update { it.copy(description = value) }

    fun addReminder() = update {
        val id = (it.reminders.maxOfOrNull { r -> r.id } ?: 0L) + 1
        it.copy(reminders = it.reminders + ReminderDraft(id = id, hour = defaultReminderHour, minute = defaultReminderMinute, message = "", enabled = true))
    }
    fun updateReminder(index: Int, reminder: ReminderDraft) = update {
        it.copy(reminders = it.reminders.toMutableList().also { l -> l[index] = reminder })
    }
    fun removeReminder(index: Int) = update {
        it.copy(reminders = it.reminders.toMutableList().also { l -> l.removeAt(index) })
    }

    private fun update(transform: (AddEditHabitUiState) -> AddEditHabitUiState) {
        _uiState.update(transform)
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val existing = existingHabit
            val habit = if (existing != null) {
                existing.copy(
                    name = state.name.trim(),
                    description = state.description.trim().ifBlank { null },
                    icon = HabitIcon.Emoji(state.emoji ?: DEFAULT_EMOJI),
                    color = state.colorArgb,
                    type = buildType(state),
                    schedule = buildSchedule(state),
                    categoryTag = state.categoryTag.trim().ifBlank { null },
                )
            } else {
                Habit(
                    id = 0,
                    name = state.name.trim(),
                    description = state.description.trim().ifBlank { null },
                    icon = HabitIcon.Emoji(state.emoji ?: DEFAULT_EMOJI),
                    color = state.colorArgb,
                    type = buildType(state),
                    schedule = buildSchedule(state),
                    categoryTag = state.categoryTag.trim().ifBlank { null },
                    isArchived = false,
                    vacationRange = null,
                    sortOrder = habitRepository.getNextSortOrder(),
                    createdAtEpochDay = LocalDate.now().toEpochDay(),
                )
            }
            val id = if (existing != null) {
                habitRepository.updateHabit(habit)
                habit.id
            } else {
                habitRepository.createHabit(habit)
            }
            val reminders = state.reminders.map {
                Reminder(id = it.id, habitId = id, hour = it.hour, minute = it.minute, message = it.message.ifBlank { null }, isEnabled = it.enabled)
            }
            habitRepository.saveReminders(id, reminders)
            notificationScheduler.rescheduleAll()
            _saved.send(existing != null)
        }
    }

    private fun buildType(state: AddEditHabitUiState): HabitType = when (state.type) {
        FormHabitType.BINARY -> HabitType.Binary
        FormHabitType.AMOUNT -> HabitType.Amount(
            goal = state.amountGoalText.toDoubleOrNull() ?: 0.0,
            unit = state.amountUnit.trim(),
        )
        FormHabitType.CHECKLIST -> HabitType.Checklist(
            steps = state.checklistSteps.map { it.trim() }.filter { it.isNotBlank() },
        )
    }

    private fun buildSchedule(state: AddEditHabitUiState): HabitSchedule = when (state.scheduleKind) {
        ScheduleKind.DAILY -> HabitSchedule.Daily
        ScheduleKind.SPECIFIC_WEEKDAYS -> HabitSchedule.SpecificWeekdays(
            state.selectedWeekdays.map { DayOfWeek.of(it) }.toSet()
        )
        ScheduleKind.TIMES_PER_WEEK -> HabitSchedule.TimesPerWeek(state.timesPerWeekText.toIntOrNull() ?: 1)
        ScheduleKind.TIMES_PER_MONTH -> HabitSchedule.TimesPerMonth(state.timesPerMonthText.toIntOrNull() ?: 1)
        ScheduleKind.EVERY_N_DAYS -> HabitSchedule.EveryNDays(state.everyNDaysText.toIntOrNull() ?: 1)
    }

    companion object {
        private const val DEFAULT_EMOJI = "\u2B50"
    }
}
