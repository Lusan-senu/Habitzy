package com.htj.habitzy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

    @Test
    fun `per-habit ranking is sorted descending by completion rate`() {
        val h1 = habit(1)
        val h2 = habit(2)
        val logs = buildList {
            for (day in 1..10) {
                add(HabitLog(1, LocalDate.of(2026, 3, day), true, null, 0, null))
            }
            add(HabitLog(2, today, true, null, 0, null))
        }

        val result = useCase.invoke(listOf(h1, h2), logs, today, InsightPeriod.MONTH)

        assertThat(result.perHabitRanking[0].habitId).isEqualTo(1L)
        assertThat(result.perHabitRanking[1].habitId).isEqualTo(2L)
    }

    @Test
    fun `all time ranking uses bounded range from earliest data`() {
        val createdAt = LocalDate.of(2026, 3, 5).toEpochDay()
        val h = Habit(
            id = 1,
            name = "H",
            description = null,
            icon = HabitIcon.Emoji("🏃"),
            color = 0xFF00A896.toInt(),
            type = HabitType.Binary,
            schedule = HabitSchedule.Daily,
            categoryTag = null,
            isArchived = false,
            vacationRange = null,
            sortOrder = 0,
            createdAtEpochDay = createdAt,
        )
        val logs = listOf(HabitLog(1, LocalDate.of(2026, 3, 5), true, null, 0, null))

        val result = useCase.invoke(listOf(h), logs, today, InsightPeriod.ALL_TIME)

        assertThat(result.perHabitRanking).hasSize(1)
        assertThat(result.perHabitRanking[0].value).isGreaterThan(0f)
    }

    @Test
    fun `hour of day histogram bins completions by completed-at hour`() {
        val logs = listOf(
            HabitLog(1, today, true, null, 0, Instant.parse("2026-03-10T08:30:00Z")),
            HabitLog(1, today.minusDays(1), true, null, 0, Instant.parse("2026-03-09T20:15:00Z")),
        )

        val result = useCase.invoke(
            listOf(habit(1)),
            logs,
            today,
            InsightPeriod.MONTH,
            zoneId = ZoneOffset.UTC,
        )

        assertThat(result.hourOfDayHistogram[8]).isEqualTo(1)
        assertThat(result.hourOfDayHistogram[20]).isEqualTo(1)
    }
}
