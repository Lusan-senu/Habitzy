package com.htj.habitzy.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import com.htj.habitzy.domain.usecase.ComputeStreakUseCase
import com.htj.habitzy.ui.components.HabitWithTodayLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val computeStreakUseCase: ComputeStreakUseCase,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val showArchived = MutableStateFlow(false)
    private val activeCategoryFilters = MutableStateFlow<Set<String>>(emptySet())
    private val sortMode = MutableStateFlow(SortMode.MANUAL)

    private val _snackbarMessages = Channel<HabitsSnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _celebrationEvents = Channel<Boolean>(Channel.CONFLATED)
    val celebrationEvents = _celebrationEvents.receiveAsFlow()

    private data class HabitAndLog(
        val habit: Habit,
        val log: HabitLog?,
        val currentStreak: Int,
    )

    private val dataFlow = combine(
        combine(
            habitRepository.observeActiveHabits(),
            habitRepository.observeArchivedHabits(),
        ) { active, archived -> active + archived },
        selectedDate,
    ) { habits, date -> habits to date }
        .flatMapLatest { (habits, date) ->
            habitRepository.observeAllLogsInRange(date.minusDays(400L)..date)
                .map { allLogs ->
                    val byHabitId = allLogs.groupBy { it.habitId }
                    habits.mapNotNull { habit ->
                        val logs = byHabitId[habit.id].orEmpty()
                        habit.id to HabitAndLog(
                            habit = habit,
                            log = logs.firstOrNull { it.date == date },
                            currentStreak = computeStreakUseCase.invoke(
                                logs = logs,
                                schedule = habit.schedule,
                                today = date,
                                vacationRange = habit.vacationRange,
                                createdAtEpochDay = habit.createdAtEpochDay,
                            ).currentStreak,
                        )
                    }.toMap()
                }
        }

    private data class LocalViewState(
        val selectedDate: LocalDate,
        val showArchived: Boolean,
        val filters: Set<String>,
        val sort: SortMode,
    )

    private val viewContext = combine(
        combine(selectedDate, showArchived, activeCategoryFilters, sortMode) {
                date, archived, filters, sort ->
            LocalViewState(date, archived, filters, sort)
        },
        settingsRepository.weekStartDay,
        settingsRepository.lastCelebratedEpochDay,
    ) { local, weekStart, lastCelebrated ->
        ViewContext(
            selectedDate = local.selectedDate,
            showArchived = local.showArchived,
            activeCategoryFilters = local.filters,
            sortMode = local.sort,
            weekStartDay = weekStart,
            lastCelebrated = lastCelebrated,
        )
    }

    val uiState: StateFlow<HabitsUiState> = combine(dataFlow, viewContext) { data, ctx ->
        buildState(ctx, data)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState(isLoading = true),
    )

    private fun buildState(ctx: ViewContext, data: Map<Long, HabitAndLog>): HabitsUiState {
        val today = LocalDate.now()

        val scheduled = data.values
            .map { it.habit }
            .filter { it.vacationRange?.let { r -> ctx.selectedDate in r } != true }
            .filter { it.schedule.isScheduledOn(ctx.selectedDate, it.createdAtEpochDay) }
            .filter { ctx.activeCategoryFilters.isEmpty() || ctx.activeCategoryFilters.contains(it.categoryTag) }
            .map { habit ->
                val entry = data[habit.id]
                val log = entry?.log
                HabitWithTodayLog(
                    habit = habit,
                    isCompletedToday = log?.isCompleted ?: false,
                    amountProgress = log?.amountValue ?: 0.0,
                    checklistDoneCount = if (habit.type is HabitType.Checklist) {
                        Integer.bitCount(log?.checklistDoneMask ?: 0)
                    } else 0,
                    currentStreak = entry?.currentStreak ?: 0,
                )
            }
            .sortedWith(habitComparator(ctx.sortMode))

        val grouped = scheduled
            .groupBy { it.habit.categoryTag }
            .map { (tag, list) -> HabitGroup(tag, list) }
            .sortedBy { if (it.categoryTag == null) 1 else 0 }

        maybeFireCelebration(ctx, scheduled, today)

        return HabitsUiState(
            isLoading = false,
            selectedDate = ctx.selectedDate,
            weekDates = weekDates(ctx.selectedDate, ctx.weekStartDay),
            weekStartDay = ctx.weekStartDay,
            habits = scheduled,
            groupedHabits = grouped,
            showArchived = ctx.showArchived,
            activeCategoryFilters = ctx.activeCategoryFilters,
            availableCategoryTags = data.values.mapNotNull { it.habit.categoryTag }.distinct().sorted(),
            sortMode = ctx.sortMode,
            hasAnyHabits = data.isNotEmpty(),
        )
    }

    private fun habitComparator(mode: SortMode): Comparator<HabitWithTodayLog> = when (mode) {
        SortMode.CURRENT_STREAK -> compareByDescending<HabitWithTodayLog> { it.currentStreak }
        SortMode.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.habit.name }
        SortMode.CREATED_DATE -> compareBy { it.habit.createdAtEpochDay }
        SortMode.MANUAL -> compareBy { it.habit.sortOrder }
    }

    private fun weekDates(selected: LocalDate, weekStart: DayOfWeek): List<LocalDate> {
        val start = selected.minusDays((selected.dayOfWeek.value - weekStart.value + 7) % 7L)
        return (0L until 7L).map { start.plusDays(it) }
    }

    private fun maybeFireCelebration(ctx: ViewContext, scheduled: List<HabitWithTodayLog>, today: LocalDate) {
        if (ctx.selectedDate != today) return
        if (scheduled.isEmpty() || !scheduled.all { it.isCompletedToday }) return
        if (ctx.lastCelebrated == today.toEpochDay()) return
        viewModelScope.launch {
            settingsRepository.setLastCelebratedEpochDay(today.toEpochDay())
            _celebrationEvents.send(true)
        }
    }

    fun onSelectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun onSelectToday() {
        selectedDate.value = LocalDate.now()
    }

    fun onToggleShowArchived(enabled: Boolean) {
        showArchived.value = enabled
    }

    fun onToggleCategoryFilter(tag: String) {
        val current = activeCategoryFilters.value
        activeCategoryFilters.value = if (tag in current) current - tag else current + tag
    }

    fun onSelectSortMode(mode: SortMode) {
        sortMode.value = mode
    }

    fun onToggleCompletion(habitId: Long, date: LocalDate) {
        viewModelScope.launch {
            habitRepository.toggleCompletion(habitId, date)
            _snackbarMessages.send(
                HabitsSnackbarMessage(
                    text = "Habit updated",
                    actionLabel = "Undo",
                    action = HabitsSnackbarAction.UndoToggle(habitId, date),
                )
            )
        }
    }

    fun onAmountStep(habitId: Long, date: LocalDate, currentValue: Double, delta: Double) {
        val next = (currentValue + delta).coerceAtLeast(0.0)
        viewModelScope.launch {
            habitRepository.setAmountProgress(habitId, date, next)
        }
    }

    fun onChecklistStep(habitId: Long, date: LocalDate, stepIndex: Int, done: Boolean) {
        viewModelScope.launch {
            habitRepository.setChecklistStep(habitId, date, stepIndex, done)
        }
    }

    fun onUndoToggle(habitId: Long, date: LocalDate) {
        viewModelScope.launch {
            habitRepository.toggleCompletion(habitId, date)
        }
    }

    fun onReorder(newOrder: List<Long>) {
        viewModelScope.launch {
            habitRepository.reorderHabits(newOrder)
        }
    }

    fun onArchive(habitId: Long) {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId, true)
            _snackbarMessages.send(
                HabitsSnackbarMessage(text = "Habit archived", actionLabel = "Undo")
            )
        }
    }

    private data class ViewContext(
        val selectedDate: LocalDate,
        val showArchived: Boolean,
        val activeCategoryFilters: Set<String>,
        val sortMode: SortMode,
        val weekStartDay: DayOfWeek,
        val lastCelebrated: Long?,
    )
}
