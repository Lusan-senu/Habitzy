package com.htj.habitzy.ui.habitdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.InsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val insightsRepository: InsightsRepository,
) : ViewModel() {

    private val habitId: Long = savedStateHandle["habitId"] ?: 0L

    private val range = MutableStateFlow(DetailRange.MONTH)
    private val ratePeriod = MutableStateFlow(RatePeriod.D30)

    private val _snackbar = Channel<DetailSnackbarMessage>(Channel.BUFFERED)
    val snackbar = _snackbar.receiveAsFlow()

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitRepository.observeHabit(habitId),
        insightsRepository.observeStreakInfo(habitId),
        habitRepository.observeLogs(habitId, LocalDate.now().minusDays(364)..LocalDate.now()),
        habitRepository.observeNotes(habitId),
        combine(range, ratePeriod) { r, p -> r to p },
    ) { habit, streakInfo, logs, notes, (rng, rate) ->
        val completionLogs = logs.associateBy { it.date }
        HabitDetailUiState(
            isLoading = false,
            habit = habit,
            streakInfo = streakInfo,
            completionLogs = completionLogs,
            notes = notes,
            range = rng,
            ratePeriod = rate,
            weeklyCompletionCounts = weeklyCounts(logs),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitDetailUiState(isLoading = true),
    )

    fun onSelectRange(r: DetailRange) {
        range.value = r
    }

    fun onSelectRatePeriod(p: RatePeriod) {
        ratePeriod.value = p
    }

    fun onToggleDay(date: LocalDate) {
        viewModelScope.launch {
            habitRepository.toggleCompletion(habitId, date)
        }
    }

    fun onAmountStep(date: LocalDate, currentValue: Double, delta: Double) {
        val next = (currentValue + delta).coerceAtLeast(0.0)
        viewModelScope.launch {
            habitRepository.setAmountProgress(habitId, date, next)
        }
    }

    fun onChecklistStep(date: LocalDate, stepIndex: Int, done: Boolean) {
        viewModelScope.launch {
            habitRepository.setChecklistStep(habitId, date, stepIndex, done)
        }
    }

    fun onSaveDayNote(date: LocalDate, text: String, photoUri: String?) {
        viewModelScope.launch {
            habitRepository.setDayNote(habitId, date, text, photoUri)
        }
    }

    fun onArchive() {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId, true)
            _snackbar.send(
                DetailSnackbarMessage(
                    text = "Habit archived",
                    actionLabel = "Undo",
                    action = DetailSnackbarMessage.Unarchive,
                )
            )
        }
    }

    fun onUnarchive() {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId, false)
            _snackbar.send(
                DetailSnackbarMessage(
                    text = "Habit restored",
                    actionLabel = "Undo",
                    action = DetailSnackbarMessage.Archive,
                )
            )
        }
    }

    fun onArchiveUndo() {
        viewModelScope.launch { habitRepository.archiveHabit(habitId, true) }
    }

    fun onUnarchiveUndo() {
        viewModelScope.launch { habitRepository.archiveHabit(habitId, false) }
    }

    fun onDeletePermanently() {
        viewModelScope.launch {
            habitRepository.deleteHabitPermanently(habitId)
        }
    }

    fun onSetVacation(start: LocalDate, endInclusive: LocalDate) {
        viewModelScope.launch {
            // Vacation is stored on the Habit entity; update it preserving all else.
            val habit = (uiState.value.habit) ?: return@launch
            val updated = habit.copy(vacationRange = start..endInclusive)
            habitRepository.updateHabit(updated)
        }
    }

    fun onClearVacation() {
        viewModelScope.launch {
            val habit = (uiState.value.habit) ?: return@launch
            habitRepository.updateHabit(habit.copy(vacationRange = null))
        }
    }

    private fun weeklyCounts(logs: List<com.htj.habitzy.domain.model.HabitLog>): List<Int> {
        val start = LocalDate.now().minusWeeks(11)
        val result = mutableListOf<Int>()
        var week = start
        while (week <= LocalDate.now()) {
            val weekEnd = week.plusDays(6).coerceAtMost(LocalDate.now())
            val count = logs.count { it.isCompleted && it.date in week..weekEnd }
            result.add(count)
            week = week.plusWeeks(1)
        }
        return result
    }
}
