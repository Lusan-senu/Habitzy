package com.htj.habitzy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

class ComputeStreakUseCaseTest {

    private val useCase = ComputeStreakUseCase()

    private fun log(
        habitId: Long = 1,
        date: LocalDate,
        completed: Boolean = true,
    ) = HabitLog(habitId, date, completed, null, 0, null)

    @Test
    fun `daily habit with one gap breaks current streak but keeps best streak`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.Daily
        val logs = (0..9).mapNotNull { offset ->
            val date = today.minusDays(offset.toLong())
            val completed = offset < 3 || offset in 4..8
            if (offset == 3) null else log(date = date, completed = completed)
        }

        val result = useCase.invoke(logs = logs, schedule = schedule, today = today)

        assertThat(result.currentStreak).isEqualTo(3)
        assertThat(result.bestStreak).isEqualTo(5)
    }

    @Test
    fun `every-3-days habit only expects the rolling due date`() {
        val today = LocalDate.of(2026, 3, 10)
        val createdAt = today.minusDays(9).toEpochDay() // so today is exactly due at multiples of 3
        val schedule = HabitSchedule.EveryNDays(3)
        val logs = listOf(
            log(date = today.minusDays(6), completed = true),
            log(date = today.minusDays(3), completed = true),
        )

        val result = useCase.invoke(
            logs = logs,
            schedule = schedule,
            today = today,
            createdAtEpochDay = createdAt,
        )

        assertThat(result.currentStreak).isEqualTo(2)
    }

    @Test
    fun `vacation range excludes those days from both streak and rate math`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.Daily
        val vacation = LocalDate.of(2026, 3, 5)..LocalDate.of(2026, 3, 7)
        val logs = (0..9).map { offset ->
            val date = today.minusDays(offset.toLong())
            log(date = date, completed = date !in vacation)
        }

        val result = useCase.invoke(
            logs = logs,
            schedule = schedule,
            today = today,
            vacationRange = vacation,
        )

        assertThat(result.currentStreak).isEqualTo(10)
    }

    @Test
    fun `specific weekdays only counts scheduled days`() {
        val today = LocalDate.of(2026, 3, 10) // Tuesday
        val schedule = HabitSchedule.SpecificWeekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        // Monday(3/9) not logged -> breaks current streak even though Tuesday isn't expected
        val logs = listOf(
            log(date = today.minusDays(5), completed = true), // Thu 3/5 not expected, ignored
            log(date = today.minusDays(4), completed = true), // Fri 3/6 expected
            log(date = today.minusDays(3), completed = true), // Sat 3/7 not expected
            log(date = today.minusDays(2), completed = true), // Sun 3/8 not expected
            log(date = today.minusDays(1), completed = false), // Mon 3/9 expected but missed
        )

        val result = useCase.invoke(logs = logs, schedule = schedule, today = today)

        assertThat(result.currentStreak).isEqualTo(0)
        assertThat(result.bestStreak).isEqualTo(1)
    }

    @Test
    fun `times-per-week streak counts completed periods across boundary`() {
        val useCase = ComputeStreakUseCase()
        val today = LocalDate.of(2026, 3, 15) // Sunday
        val schedule = HabitSchedule.TimesPerWeek(target = 3)
        // Previous week (Mar 2-8): 3 completions -> target met
        // Current week (Mar 9-15): 3 completions -> target met
        val completedDates = mutableListOf<LocalDate>()
        // Week Mar 2-8 (Monday start)
        for (offset in intArrayOf(7, 8, 9)) completedDates.add(today.minusDays(offset.toLong()))
        // Week Mar 9-15
        for (offset in intArrayOf(1, 2, 3)) completedDates.add(today.minusDays(offset.toLong()))

        val logs = completedDates.map { log(date = it, completed = true) }
        val result = useCase.invoke(logs = logs, schedule = schedule, today = today)

        assertThat(result.currentStreak).isEqualTo(2)
    }

    @Test
    fun `daily streak in progress not broken by unlogged today`() {
        val today = LocalDate.of(2026, 3, 10)
        val logs = (1..4).map { log(date = today.minusDays(it.toLong()), completed = true) }

        val result = useCase.invoke(logs = logs, schedule = HabitSchedule.Daily, today = today)

        assertThat(result.currentStreak).isEqualTo(4)
    }

    @Test
    fun `times-per-month counts consecutive target months`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.TimesPerMonth(target = 2)
        val logs = listOf(
            log(date = LocalDate.of(2026, 1, 5), completed = true),
            log(date = LocalDate.of(2026, 1, 20), completed = true),
            log(date = LocalDate.of(2026, 2, 5), completed = true),
            log(date = LocalDate.of(2026, 2, 20), completed = true),
            log(date = LocalDate.of(2026, 3, 2), completed = true),
            log(date = LocalDate.of(2026, 3, 8), completed = true),
        )

        val result = useCase.invoke(logs = logs, schedule = schedule, today = today)

        assertThat(result.currentStreak).isEqualTo(3)
        assertThat(result.bestStreak).isEqualTo(3)
    }

    @Test
    fun `times-per-month with unmet target has zero current streak`() {
        val today = LocalDate.of(2026, 3, 10)
        val logs = listOf(log(date = LocalDate.of(2026, 3, 2), completed = true))

        val result = useCase.invoke(logs = logs, schedule = HabitSchedule.TimesPerMonth(2), today = today)

        assertThat(result.currentStreak).isEqualTo(0)
    }

    @Test
    fun `strength score is full when every expected day completed`() {
        val today = LocalDate.of(2026, 3, 10)
        val createdAtEpochDay = today.minusDays(90).toEpochDay()
        val logs = (0 until 60).map { log(date = today.minusDays(it.toLong()), completed = true) }

        val result = useCase.invoke(
            logs = logs,
            schedule = HabitSchedule.Daily,
            today = today,
            createdAtEpochDay = createdAtEpochDay,
        )

        assertThat(result.strengthScore).isEqualTo(100f)
    }

    @Test
    fun `strength score is zero when nothing completed`() {
        val today = LocalDate.of(2026, 3, 10)
        val logs = (0 until 60).map { log(date = today.minusDays(it.toLong()), completed = false) }

        val result = useCase.invoke(logs = logs, schedule = HabitSchedule.Daily, today = today)

        assertThat(result.strengthScore).isEqualTo(0f)
    }

    @Test
    fun `strength score is null when fewer than five expected days`() {
        val today = LocalDate.of(2026, 3, 10)

        val result = useCase.invoke(
            logs = emptyList(),
            schedule = HabitSchedule.EveryNDays(100),
            today = today,
        )

        assertThat(result.strengthScore).isNull()
    }
}
