package com.htj.habitzy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

/**
 * Supplemental §11.4 coverage for the global insights aggregation shared with Habit Detail.
 */
class ComputeInsightsUseCaseSupplementalTest {

    private val useCase = ComputeInsightsUseCase()
    private val today = LocalDate.of(2026, 3, 10)

    private fun habit(
        id: Long,
        schedule: HabitSchedule = HabitSchedule.Daily,
        vacationRange: ClosedRange<LocalDate>? = null,
        createdAt: LocalDate = today.minusDays(30),
    ) = Habit(
        id = id,
        name = "Habit $id",
        description = null,
        icon = HabitIcon.Emoji("🏃"),
        color = 0xFF00A896.toInt(),
        type = HabitType.Binary,
        schedule = schedule,
        categoryTag = null,
        isArchived = false,
        vacationRange = vacationRange,
        sortOrder = 0,
        createdAtEpochDay = createdAt.toEpochDay(),
    )

    private fun log(habitId: Long, date: LocalDate, completed: Boolean = true) =
        HabitLog(habitId, date, completed, null, 0, null)

    @Test
    fun `weekly trend counts completions per full iso week for 12 weeks`() {
        val habit = habit(1)
        // One completion every Monday for the last 12 ISO weeks.
        val logs = (1..12).map { weeksAgo ->
            val monday = today.minusWeeks(weeksAgo.toLong()).with(DayOfWeek.MONDAY)
            log(1, monday)
        }

        val result = useCase.invoke(listOf(habit), logs, today, InsightPeriod.WEEK)

        assertThat(result.weeklyCompletionsTrend).hasSize(12)
        assertThat(result.weeklyCompletionsTrend.sum()).isEqualTo(12)
    }

    @Test
    fun `monthly heatmap is zero for days with no scheduled habits`() {
        // A Monday-only habit in a month where the 1st falls on a Tuesday-ish…
        // simpler: SpecificWeekdays(SUNDAY) with zero completions.
        val habit = habit(1, schedule = HabitSchedule.SpecificWeekdays(setOf(DayOfWeek.SUNDAY)))
        val result = useCase.invoke(listOf(habit), emptyList(), today, InsightPeriod.MONTH)

        assertThat(result.monthlyHeatmap).hasSize(today.lengthOfMonth())
        result.monthlyHeatmap.forEachIndexed { index, rate ->
            val date = today.withDayOfMonth(index + 1)
            if (date.dayOfWeek == DayOfWeek.SUNDAY && !date.isAfter(today)) {
                assertThat(rate).isEqualTo(0f)
            }
        }
    }

    @Test
    fun `monthly heatmap is one when every scheduled habit is completed that day`() {
        val h1 = habit(1)
        val h2 = habit(2)
        val date = today.minusDays(1)
        val logs = listOf(log(1, date), log(2, date))

        val result = useCase.invoke(listOf(h1, h2), logs, today, InsightPeriod.MONTH)

        assertThat(result.monthlyHeatmap[date.dayOfMonth - 1]).isEqualTo(1f)
    }

    @Test
    fun `monthly heatmap zero rate does not divide by zero on scheduled-but-unscheduled days`() {
        val result = useCase.invoke(emptyList(), emptyList(), today, InsightPeriod.MONTH)
        assertThat(result.monthlyHeatmap).hasSize(today.lengthOfMonth())
        assertThat(result.monthlyHeatmap.all { it == 0f }).isTrue()
    }

    @Test
    fun `week range respects custom week start day`() {
        val habit = habit(1)
        val saturdayStart = DayOfWeek.SATURDAY
        // Today Tue 2026-03-10. Week starting Saturday = Mar 7..Mar 13.
        val logs = listOf(
            log(1, LocalDate.of(2026, 3, 6)), // Friday — outside a Sat-start week
            log(1, LocalDate.of(2026, 3, 7)), // Saturday — inside
        )

        val result = useCase.invoke(
            listOf(habit), logs, today, InsightPeriod.WEEK, weekStart = saturdayStart,
        )

        // 7 expected days (Mar 7..13), 1 completed
        assertThat(result.overallCompletionRateWeek).isEqualTo(1f / 7f)
    }

    @Test
    fun `overall today rate counts only scheduled habits for today`() {
        val daily = habit(1)
        val tuesdaysOnly = habit(2, schedule = HabitSchedule.SpecificWeekdays(setOf(DayOfWeek.TUESDAY)))
        // Only habit 2 completed today; habit 1 (daily) missed.
        val logs = listOf(log(2, today))

        val result = useCase.invoke(listOf(daily, tuesdaysOnly), logs, today, InsightPeriod.TODAY)

        assertThat(result.overallCompletionRateToday).isEqualTo(0.5f)
    }

    @Test
    fun `distinct logged days feeds has-insight-data gating`() {
        val habit = habit(1)
        val logs = listOf(log(1, today.minusDays(1)), log(1, today.minusDays(2)), log(1, today.minusDays(3)))

        val result = useCase.invoke(listOf(habit), logs, today, InsightPeriod.ALL_TIME)

        assertThat(result.distinctLoggedDays).isEqualTo(3)
    }

    @Test
    fun `back-filled logs without completed-at are excluded from hour histogram`() {
        val habit = habit(1)
        val logs = listOf(
            HabitLog(1, today, true, null, 0, null), // no timestamp — must be skipped
        )

        val result = useCase.invoke(listOf(habit), logs, today, InsightPeriod.MONTH)

        assertThat(result.hourOfDayHistogram.sum()).isEqualTo(0)
    }
}
