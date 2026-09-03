package com.htj.habitzy.ui.insights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.domain.model.InsightSummary
import com.htj.habitzy.domain.model.StreakInfo
import com.htj.habitzy.domain.repository.InsightsRepository
import com.htj.habitzy.domain.usecase.InsightPeriod
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private class FakeInsightsRepository(periods: MutableStateFlow<InsightSummary>) : InsightsRepository {
        val periods = periods
        var lastRequestedPeriod: InsightPeriod? = null
        override fun observeStreakInfo(habitId: Long): Flow<StreakInfo> =
            MutableStateFlow(StreakInfo(0, 0, 0, 0f, 0f, 0f, 0f, null))
        override fun observeGlobalInsights(period: InsightPeriod): Flow<InsightSummary> {
            lastRequestedPeriod = period
            return periods
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun summary() = InsightSummary(
        overallCompletionRateToday = 1f,
        overallCompletionRateWeek = 0.8f,
        overallCompletionRateMonth = 0.6f,
        overallCompletionRateAllTime = 0.5f,
        perHabitRanking = emptyList(),
        hourOfDayHistogram = IntArray(24),
        bestCurrentStreaks = emptyList(),
        weeklyCompletionsTrend = List(12) { 0 },
        monthlyHeatmap = emptyList(),
        distinctLoggedDays = 10,
    )

    @Test
    fun `selecting a period re-requests insights for that period`() = runTest(testDispatcher) {
        val repo = FakeInsightsRepository(MutableStateFlow(summary()))
        val vm = InsightsViewModel(repo)

        val states = mutableListOf<InsightsUiState>()
        val collector = launch(testDispatcher) { vm.uiState.collect { states.add(it) } }
        runCurrent()

        assertThat(repo.lastRequestedPeriod).isEqualTo(InsightPeriod.WEEK)

        vm.selectPeriod(InsightPeriod.MONTH)
        runCurrent()

        assertThat(repo.lastRequestedPeriod).isEqualTo(InsightPeriod.MONTH)

        vm.selectPeriod(InsightPeriod.TODAY)
        runCurrent()

        assertThat(repo.lastRequestedPeriod).isEqualTo(InsightPeriod.TODAY)
        assertThat(states.last().period).isEqualTo(InsightPeriod.TODAY)

        collector.cancel()
    }

    @Test
    fun `focused habit updates without changing period`() = runTest(testDispatcher) {
        val repo = FakeInsightsRepository(MutableStateFlow(summary()))
        val vm = InsightsViewModel(repo)

        val states = mutableListOf<InsightsUiState>()
        val collector = launch(testDispatcher) { vm.uiState.collect { states.add(it) } }
        runCurrent()

        vm.selectHabit(42L)
        runCurrent()

        assertThat(states.last().focusedHabitId).isEqualTo(42L)
        assertThat(repo.lastRequestedPeriod).isEqualTo(InsightPeriod.WEEK)

        collector.cancel()
    }
}
