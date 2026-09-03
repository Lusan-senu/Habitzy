package com.htj.habitzy.domain.usecase

import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.StreakInfo
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.pow

/**
 * Pure function computing streak and completion-rate statistics from raw logs.
 * Vacation-range days are removed from consideration entirely before math runs.
 */
class ComputeStreakUseCase @Inject constructor() {

    fun invoke(
        logs: List<HabitLog>,
        schedule: HabitSchedule,
        today: LocalDate,
        vacationRange: ClosedRange<LocalDate>? = null,
        createdAtEpochDay: Long = today.toEpochDay(),
    ): StreakInfo {
        val logByDay = logs
            .filter { vacationRange == null || it.date !in vacationRange }
            .associateBy { it.date.toEpochDay() }

        val isPeriod = schedule is HabitSchedule.TimesPerWeek || schedule is HabitSchedule.TimesPerMonth

        val current = computeCurrentStreak(logByDay, schedule, today, createdAtEpochDay, vacationRange)
        val best = computeBestStreak(logByDay, schedule, today, createdAtEpochDay, vacationRange)
        val totalCompletions = logByDay.count { it.value.isCompleted }

        val rate7d = if (isPeriod) 0f else completionRate(logByDay, schedule, today, 7, createdAtEpochDay, vacationRange)
        val rate30d = if (isPeriod) 0f else completionRate(logByDay, schedule, today, 30, createdAtEpochDay, vacationRange)
        val rate90d = if (isPeriod) 0f else completionRate(logByDay, schedule, today, 90, createdAtEpochDay, vacationRange)
        val rateAllTime = if (isPeriod) 0f else completionRateAllTime(logByDay)
        val strength = if (isPeriod) null else computeStrength(logByDay, schedule, today, createdAtEpochDay, vacationRange)

        return StreakInfo(
            currentStreak = current,
            bestStreak = best,
            totalCompletions = totalCompletions,
            completionRate7d = rate7d,
            completionRate30d = rate30d,
            completionRate90d = rate90d,
            completionRateAllTime = rateAllTime,
            strengthScore = strength,
        )
    }

    private fun onVacation(date: LocalDate, vacationRange: ClosedRange<LocalDate>?): Boolean =
        vacationRange?.let { date in it } == true

    private fun isExpectedDay(date: LocalDate, schedule: HabitSchedule, createdAtEpochDay: Long): Boolean {
        return when (schedule) {
            is HabitSchedule.Daily -> true
            is HabitSchedule.SpecificWeekdays -> schedule.weekdays.contains(date.dayOfWeek)
            is HabitSchedule.EveryNDays -> {
                val daysSince = date.toEpochDay() - createdAtEpochDay
                daysSince >= 0 && daysSince % schedule.n == 0L
            }
            is HabitSchedule.TimesPerWeek, is HabitSchedule.TimesPerMonth -> true
        }
    }

    private fun computeCurrentStreak(
        logByDay: Map<Long, HabitLog>,
        schedule: HabitSchedule,
        today: LocalDate,
        createdAtEpochDay: Long,
        vacationRange: ClosedRange<LocalDate>?,
    ): Int = when (schedule) {
        is HabitSchedule.TimesPerWeek -> currentWeekStreak(logByDay, schedule.target, today)
        is HabitSchedule.TimesPerMonth -> currentMonthStreak(logByDay, schedule.target, today)
        else -> {
            var day = today
            var streak = 0
            val earliestEpochDay = logByDay.keys.minOrNull() ?: createdAtEpochDay
            while (day.toEpochDay() >= earliestEpochDay) {
                if (onVacation(day, vacationRange)) {
                    // A vacation day is a free day: it neither requires completion nor breaks the streak,
                    // but it still counts toward the streak's calendar-day length.
                    streak++
                    day = day.minusDays(1)
                    continue
                }
                if (!isExpectedDay(day, schedule, createdAtEpochDay)) {
                    day = day.minusDays(1)
                    continue
                }
                val log = logByDay[day.toEpochDay()]
                if (log == null) {
                    // Today being unresolved does not break the streak; a past expected day that
                    // was never logged counts as missed and breaks it.
                    if (day == today) {
                        day = day.minusDays(1)
                        continue
                    } else {
                        break
                    }
                } else if (log.isCompleted) {
                    streak++
                    day = day.minusDays(1)
                } else {
                    break
                }
            }
            streak
        }
    }

    private fun computeBestStreak(
        logByDay: Map<Long, HabitLog>,
        schedule: HabitSchedule,
        today: LocalDate,
        createdAtEpochDay: Long,
        vacationRange: ClosedRange<LocalDate>?,
    ): Int = when (schedule) {
        is HabitSchedule.TimesPerWeek -> bestWeekStreak(logByDay, schedule.target, today)
        is HabitSchedule.TimesPerMonth -> bestMonthStreak(logByDay, schedule.target, today)
        else -> {
            var best = 0
            var current = 0
            var day = today
            val earliest = logByDay.keys.minOrNull() ?: createdAtEpochDay
            while (day.toEpochDay() >= earliest) {
                if (onVacation(day, vacationRange)) {
                    day = day.minusDays(1)
                    continue
                }
                if (!isExpectedDay(day, schedule, createdAtEpochDay)) {
                    day = day.minusDays(1)
                    continue
                }
                val log = logByDay[day.toEpochDay()]
                if (log != null && log.isCompleted) {
                    current++
                    if (current > best) best = current
                } else {
                    current = 0
                }
                day = day.minusDays(1)
            }
            best
        }
    }

    private fun completionRate(
        logByDay: Map<Long, HabitLog>,
        schedule: HabitSchedule,
        today: LocalDate,
        days: Int,
        createdAtEpochDay: Long,
        vacationRange: ClosedRange<LocalDate>?,
    ): Float {
        var expected = 0
        var completed = 0
        for (offset in 0 until days) {
            val date = today.minusDays(offset.toLong())
            if (onVacation(date, vacationRange)) continue
            if (!isExpectedDay(date, schedule, createdAtEpochDay)) continue
            expected++
            val log = logByDay[date.toEpochDay()]
            if (log != null && log.isCompleted) completed++
        }
        return if (expected == 0) 0f else completed.toFloat() / expected
    }

    private fun completionRateAllTime(logByDay: Map<Long, HabitLog>): Float {
        val expected = logByDay.size
        val completed = logByDay.count { it.value.isCompleted }
        return if (expected == 0) 0f else completed.toFloat() / expected
    }

    private fun computeStrength(
        logByDay: Map<Long, HabitLog>,
        schedule: HabitSchedule,
        today: LocalDate,
        createdAtEpochDay: Long,
        vacationRange: ClosedRange<LocalDate>?,
    ): Float? {
        val expectedDays = mutableListOf<LocalDate>()
        var day = today
        var safety = 0
        while (expectedDays.size < 60 && safety < 2000) {
            safety++
            if (onVacation(day, vacationRange)) {
                day = day.minusDays(1)
                continue
            }
            if (isExpectedDay(day, schedule, createdAtEpochDay)) expectedDays.add(day)
            day = day.minusDays(1)
        }
        if (expectedDays.size < 5) return null

        var weightedSum = 0.0
        var weightSum = 0.0
        expectedDays.forEachIndexed { index, date ->
            val weight = 0.5.pow(index / 20.0)
            val log = logByDay[date.toEpochDay()]
            if (log != null && log.isCompleted) weightedSum += weight
            weightSum += weight
        }
        if (weightSum == 0.0) return null
        return (100.0 * weightedSum / weightSum).toFloat().coerceIn(0f, 100f)
    }

    // --- Week-based helpers ---

    private fun weekStarting(date: LocalDate): LocalDate = date.minusDays(date.dayOfWeek.value - 1L)

    private fun completionsInWeek(weekStart: LocalDate, logByDay: Map<Long, HabitLog>): Int {
        val start = weekStart.toEpochDay()
        return logByDay.values.count { log ->
            log.isCompleted && log.date.toEpochDay() in start until (start + 7)
        }
    }

    private fun currentWeekStreak(logByDay: Map<Long, HabitLog>, target: Int, today: LocalDate): Int {
        var weekStart = weekStarting(today)
        var streak = 0
        var first = true
        while (true) {
            val count = completionsInWeek(weekStart, logByDay)
            if (count >= target) {
                streak++
                weekStart = weekStart.minusDays(7)
            } else if (first) {
                break
            } else {
                break
            }
            first = false
        }
        return streak
    }

    private fun bestWeekStreak(logByDay: Map<Long, HabitLog>, target: Int, today: LocalDate): Int {
        val weeks = logByDay.keys.map { weekStarting(LocalDate.ofEpochDay(it)) }.distinct().sorted()
        if (weeks.isEmpty()) return 0
        var best = 0
        var current = 0
        var prev: LocalDate? = null
        for (weekStart in weeks) {
            val ok = completionsInWeek(weekStart, logByDay) >= target
            if (ok && (prev == null || weekStart == prev.plusDays(7))) {
                current++
            } else if (ok) {
                current = 1
            } else {
                current = 0
            }
            if (current > best) best = current
            prev = weekStart
        }
        return best
    }

    // --- Month-based helpers ---

    private fun completionsInMonth(month: Pair<Int, Int>, logByDay: Map<Long, HabitLog>): Int {
        return logByDay.values.count { log -> log.isCompleted && monthOf(log.date) == month }
    }

    private fun monthOf(date: LocalDate): Pair<Int, Int> = date.year to date.monthValue

    private fun currentMonthStreak(logByDay: Map<Long, HabitLog>, target: Int, today: LocalDate): Int {
        val thisMonth = monthOf(today)
        var month = LocalDate.of(thisMonth.first, thisMonth.second, 1)
        var streak = 0
        var first = true
        while (true) {
            val key = monthOf(month)
            val count = completionsInMonth(key, logByDay)
            if (count >= target) {
                streak++
                month = month.minusMonths(1)
            } else if (first) {
                break
            } else {
                break
            }
            first = false
        }
        return streak
    }

    private fun bestMonthStreak(logByDay: Map<Long, HabitLog>, target: Int, today: LocalDate): Int {
        val months = logByDay.keys.map { MonthKey.of(LocalDate.ofEpochDay(it)) }.distinct().sorted()
        if (months.isEmpty()) return 0
        var best = 0
        var current = 0
        var prev: MonthKey? = null
        for (key in months) {
            val ok = completionsInMonth(key.toPair(), logByDay) >= target
            if (ok && (prev == null || key == prev.next())) {
                current++
            } else if (ok) {
                current = 1
            } else {
                current = 0
            }
            if (current > best) best = current
            prev = key
        }
        return best
    }

    private data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
        override fun compareTo(other: MonthKey): Int {
            val byYear = year.compareTo(other.year)
            return if (byYear != 0) byYear else month.compareTo(other.month)
        }

        fun toPair(): Pair<Int, Int> = year to month

        fun next(): MonthKey =
            if (month == 12) MonthKey(year + 1, 1) else MonthKey(year, month + 1)

        companion object {
            fun of(date: LocalDate): MonthKey = MonthKey(date.year, date.monthValue)
        }
    }
}
