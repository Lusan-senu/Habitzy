package com.htj.habitzy.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

sealed class HabitIcon {
    data class Emoji(val char: String) : HabitIcon()
    data class Bundled(val key: String) : HabitIcon()

    companion object {
        fun fromKey(key: String): HabitIcon =
            if (key.startsWith("emoji:")) Emoji(key.removePrefix("emoji:"))
            else Bundled(key.removePrefix("icon:"))

        fun toKey(icon: HabitIcon): String = when (icon) {
            is Emoji -> "emoji:${icon.char}"
            is Bundled -> "icon:${icon.key}"
        }
    }
}

sealed class HabitType {
    object Binary : HabitType()
    data class Amount(val goal: Double, val unit: String) : HabitType()
    data class Checklist(val steps: List<String>) : HabitType()
}

sealed class HabitSchedule {
    object Daily : HabitSchedule()
    data class SpecificWeekdays(val weekdays: Set<DayOfWeek>) : HabitSchedule()
    data class TimesPerWeek(val target: Int) : HabitSchedule()
    data class TimesPerMonth(val target: Int) : HabitSchedule()
    data class EveryNDays(val n: Int) : HabitSchedule()

    fun summaryText(): String = when (this) {
        is Daily -> "Every day"
        is SpecificWeekdays -> weekdays
            .sortedBy { it.value }
            .joinToString(", ") { it.name.take(3) }
        is TimesPerWeek -> "$target times a week"
        is TimesPerMonth -> "$target times a month"
        is EveryNDays -> "Every $n days"
    }

    /**
     * Whether the habit is eligible to be shown/logged on [date].
     * Times-per-week/month habits are eligible every day (the "still needs N more this
     * week/month" bookkeeping is conveyed via the streak/progress UI, not by hiding them).
     */
    fun isScheduledOn(date: LocalDate, createdAtEpochDay: Long): Boolean = when (this) {
        is Daily -> true
        is SpecificWeekdays -> weekdays.contains(date.dayOfWeek)
        is EveryNDays -> {
            val daysSince = date.toEpochDay() - createdAtEpochDay
            daysSince >= 0 && daysSince % n == 0L
        }
        is TimesPerWeek, is TimesPerMonth -> true
    }
}

data class Habit(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: HabitIcon,
    val color: Int,
    val type: HabitType,
    val schedule: HabitSchedule,
    val categoryTag: String?,
    val isArchived: Boolean,
    val vacationRange: ClosedRange<LocalDate>?,
    val sortOrder: Int,
    val createdAtEpochDay: Long,
)

data class HabitLog(
    val habitId: Long,
    val date: LocalDate,
    val isCompleted: Boolean,
    val amountValue: Double?,
    val checklistDoneMask: Int,
    val completedAt: java.time.Instant?,
)

data class Reminder(
    val id: Long,
    val habitId: Long,
    val hour: Int,
    val minute: Int,
    val message: String?,
    val isEnabled: Boolean,
)

data class DayNote(
    val id: Long,
    val habitId: Long,
    val date: LocalDate,
    val text: String,
    val photoUri: String?,
)

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val completionRate7d: Float,
    val completionRate30d: Float,
    val completionRate90d: Float,
    val completionRateAllTime: Float,
    val strengthScore: Float?,
)

data class HabitRankEntry(
    val habitId: Long,
    val name: String,
    val icon: HabitIcon,
    val color: Int,
    val value: Float,
)

data class InsightSummary(
    val overallCompletionRateToday: Float,
    val overallCompletionRateWeek: Float,
    val overallCompletionRateMonth: Float,
    val overallCompletionRateAllTime: Float,
    val perHabitRanking: List<HabitRankEntry>,
    val hourOfDayHistogram: IntArray,
    val bestCurrentStreaks: List<HabitRankEntry>,
    val weeklyCompletionsTrend: List<Int>,
    val monthlyHeatmap: List<Float>,
    val distinctLoggedDays: Int,
)

data class Profile(
    val name: String?,
    val photoUri: String?,
)
