package com.htj.habitzy.domain.usecase

import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitRankEntry
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.InsightSummary
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

enum class InsightPeriod { TODAY, WEEK, MONTH, ALL_TIME }

/**
 * Pure function aggregating global insights across all active habits.
 * Kept dependency-free so it can be unit tested in isolation.
 */
class ComputeInsightsUseCase @Inject constructor() {

    fun invoke(
        habits: List<Habit>,
        logs: List<HabitLog>,
        today: LocalDate,
        period: InsightPeriod,
        weekStart: java.time.DayOfWeek = LocalDate.now().dayOfWeek,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): InsightSummary {
        val habitById = habits.associateBy { it.id }
        val validLogs = logs.filter { it.habitId in habitById }

        val overallToday = completionRate(validLogs, habits, today..today)
        val weekRange = rangeForWeek(today, weekStart)
        val overallWeek = completionRate(validLogs, habits, weekRange)
        val monthRange = today.withDayOfMonth(1)..today.withDayOfMonth(today.lengthOfMonth())
        val overallMonth = completionRate(validLogs, habits, monthRange)

        val ranking = habits.map { habit ->
            val range = when (period) {
                InsightPeriod.TODAY -> today..today
                InsightPeriod.WEEK -> weekRange
                InsightPeriod.MONTH -> monthRange
                InsightPeriod.ALL_TIME -> LocalDate.MIN..today
            }
            val rate = completionRateForHabit(validLogs, habit, range)
            HabitRankEntry(habit.id, habit.name, habit.icon, habit.color, rate)
        }.sortedByDescending { it.value }

        val histogram = IntArray(24)
        validLogs.forEach { log ->
            log.completedAt?.let { instant ->
                val hour = ZonedDateTime.ofInstant(instant, zoneId).hour
                histogram[hour]++
            }
        }

        val bestCurrentStreaks = habits
            .map { habit ->
                val strive = ComputeStreakUseCase().invoke(
                    logs = validLogs.filter { it.habitId == habit.id },
                    schedule = habit.schedule,
                    today = today,
                    vacationRange = habit.vacationRange,
                )
                habit to strive.currentStreak
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { (habit, streak) -> HabitRankEntry(habit.id, habit.name, habit.icon, habit.color, streak.toFloat()) }

        val trend = mutableListOf<Int>()
        for (i in 12 downTo 1) {
            val weekStartDate = today.minusWeeks(i.toLong()).with(java.time.DayOfWeek.MONDAY)
            val weekEnd = weekStartDate.plusDays(6)
            trend.add(validLogs.count { it.date in weekStartDate..weekEnd && it.isCompleted })
        }

        return InsightSummary(
            overallCompletionRateToday = overallToday,
            overallCompletionRateWeek = overallWeek,
            overallCompletionRateMonth = overallMonth,
            perHabitRanking = ranking,
            hourOfDayHistogram = histogram,
            bestCurrentStreaks = bestCurrentStreaks,
            weeklyCompletionsTrend = trend,
        )
    }

    private fun rangeForWeek(date: LocalDate, weekStart: java.time.DayOfWeek): ClosedRange<LocalDate> {
        var start = date.minusDays((date.dayOfWeek.value - weekStart.value).toLong())
        if (start.isAfter(date)) start = start.minusDays(7)
        return start..start.plusDays(6)
    }

    private fun completionRate(logs: List<HabitLog>, habits: List<Habit>, range: ClosedRange<LocalDate>): Float {
        var expected = 0
        var completed = 0
        var day = range.start
        while (!day.isAfter(range.endInclusive)) {
            habits.forEach { habit ->
                if (isScheduled(habit, day)) {
                    expected++
                    if (logs.any { it.habitId == habit.id && it.date == day && it.isCompleted }) completed++
                }
            }
            day = day.plusDays(1)
        }
        return if (expected == 0) 0f else completed.toFloat() / expected
    }

    private fun completionRateForHabit(logs: List<HabitLog>, habit: Habit, range: ClosedRange<LocalDate>): Float {
        var expected = 0
        var completed = 0
        var day = range.start
        while (!day.isAfter(range.endInclusive)) {
            if (isScheduled(habit, day)) {
                expected++
                if (logs.any { it.habitId == habit.id && it.date == day && it.isCompleted }) completed++
            }
            day = day.plusDays(1)
        }
        return if (expected == 0) 0f else completed.toFloat() / expected
    }

    private fun isScheduled(habit: Habit, date: LocalDate): Boolean = when (habit.schedule) {
        is HabitSchedule.Daily -> true
        is HabitSchedule.SpecificWeekdays -> habit.schedule.weekdays.contains(date.dayOfWeek)
        is HabitSchedule.EveryNDays -> habit.vacationRange?.let { date in it } != true
        is HabitSchedule.TimesPerWeek, is HabitSchedule.TimesPerMonth -> true
    }
}
