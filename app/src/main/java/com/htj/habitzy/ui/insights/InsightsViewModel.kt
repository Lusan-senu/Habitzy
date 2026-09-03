package com.htj.habitzy.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.repository.InsightsRepository
import com.htj.habitzy.domain.usecase.InsightPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    insightsRepository: InsightsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(InsightPeriod.WEEK)
    private val focusedHabitId = MutableStateFlow<Long?>(null)

    private val weekStart = MutableStateFlow<DayOfWeek>(DayOfWeek.MONDAY)

    val uiState: StateFlow<InsightsUiState> = combine(period, focusedHabitId) { p, habitId -> p to habitId }
        .flatMapLatest { (p, habitId) ->
            insightsRepository.observeGlobalInsights(p).map { summary ->
                InsightsUiState(
                    isLoading = false,
                    period = p,
                    summary = summary,
                    focusedHabitId = habitId,
                    weekStartDay = weekStart.value,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightsUiState(),
        )

    fun selectPeriod(p: InsightPeriod) {
        period.value = p
    }

    fun selectHabit(habitId: Long?) {
        focusedHabitId.value = habitId
    }

    fun setWeekStart(day: DayOfWeek) {
        weekStart.value = day
    }
}