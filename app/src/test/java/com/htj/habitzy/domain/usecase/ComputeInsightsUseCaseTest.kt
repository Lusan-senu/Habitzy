package com.htj.habitzy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import java.time.LocalDate
import org.junit.Test

class ComputeInsightsUseCaseTest {

    private val useCase = ComputeInsightsUseCase()
    private val today = LocalDate.of(2026, 3, 10)

    private fun habit(id: Long) = Habit(
        id = id,
        name = "Habit $id",
        description = null,
        icon = HabitIcon.Emoji("🏃"),
        color = 0xFF00A896.toInt(),
        type = HabitType.Binary,
        schedule = HabitSchedule.Daily,
        categoryTag = null,
        isArchived = false,
        vacationRange = null,
        sortOrder = 0,
        createdAtEpochDay = today.minusDays(30).toEpochDay(),
    )

    @Test
    fun `today completion rate reflects today only`() {
        val habits = listOf(habit(1))
        val logs = listOf(
            HabitLog(1, today, true, null, 0, null),          // completed today
            HabitLog(1, today.minusDays(1), true, null, 0, null), // completed yesterday, ignored for TODAY
        )

        val result = useCase.invoke(habits, logs, today, InsightPeriod.TODAY)

        assertThat(result.overallCompletionRateToday).isEqualTo(1f)
        assertThat(result.perHabitRanking).hasSize(1)
        assertThat(result.hourOfDayHistogram).hasLength(24)
    }

    @Test
    fun `monthly heatmap and trend computed without crash on empty data`() {
        val result = useCase.invoke(emptyList(), emptyList(), today, InsightPeriod.MONTH)

        assertThat(result.perHabitRanking).isEmpty()
        assertThat(result.weeklyCompletionsTrend).hasSize(12)
    }
}
