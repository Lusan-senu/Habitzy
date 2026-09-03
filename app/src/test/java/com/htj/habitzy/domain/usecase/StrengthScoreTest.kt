package com.htj.habitzy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import java.time.LocalDate
import org.junit.Test

/**
 * Covers the §11.4 strength-score behavior: recency-weighted completion rate over the
 * trailing 60 expected days, weight(i) = 0.5^(i/20).
 */
class StrengthScoreTest {

    private val useCase = ComputeStreakUseCase()
    private val today = LocalDate.of(2026, 3, 10)

    private fun log(date: LocalDate, completed: Boolean = true) =
        HabitLog(1, date, completed, null, 0, null)

    @Test
    fun `strength score decreases monotonically as recent completions are removed one at a time`() {
        val createdAtEpochDay = today.minusDays(59).toEpochDay()
        val logs = (0 until 60).map { log(date = today.minusDays(it.toLong())) }

        var previous = useCase.invoke(
            logs = logs,
            schedule = HabitSchedule.Daily,
            today = today,
            createdAtEpochDay = createdAtEpochDay,
        ).strengthScore
        assertThat(previous).isEqualTo(100f)

        // Removing the most recent days peels off the highest-weighted completions first,
        // so the score must strictly decrease with every removal.
        for (removed in 1..55) {
            val remaining = logs.drop(removed)
            val score = useCase.invoke(
                logs = remaining,
                schedule = HabitSchedule.Daily,
                today = today,
                createdAtEpochDay = createdAtEpochDay,
            ).strengthScore
            assertThat(score).isLessThan(previous)
            previous = score
        }
    }

    @Test
    fun `old streak broken this week drops faster than shaky but recent history`() {
        val createdAtEpochDay = today.minusDays(59).toEpochDay()
        // Old perfect 25 days (offsets 35..59 back), then 35 missed days up to today.
        val broken = (35 until 60).map { log(date = today.minusDays(it.toLong())) }
        // Shaky: 10 of the most recent 20 days completed, nothing before.
        val shaky = (0 until 20).map { offset ->
            log(date = today.minusDays(offset.toLong()), completed = offset % 2 == 0)
        }

        val brokenScore = useCase.invoke(
            logs = broken, schedule = HabitSchedule.Daily, today = today,
            createdAtEpochDay = createdAtEpochDay,
        ).strengthScore
        val shakyScore = useCase.invoke(
            logs = shaky, schedule = HabitSchedule.Daily, today = today,
            createdAtEpochDay = createdAtEpochDay,
        ).strengthScore

        assertThat(shakyScore).isGreaterThan(brokenScore)
    }

    @Test
    fun `strength score shows not enough data with fewer than five expected days`() {
        val result = useCase.invoke(
            logs = (0 until 4).map { log(date = today.minusDays(it.toLong())) },
            schedule = HabitSchedule.Daily,
            today = today,
            createdAtEpochDay = today.minusDays(3).toEpochDay(),
        )

        assertThat(result.strengthScore).isNull()
    }

    @Test
    fun `strength score weights recent half-week over month-old days`() {
        val createdAtEpochDay = today.minusDays(59).toEpochDay()
        // Only the 5 most recent days completed vs only 5 days completed 40 days ago.
        val recent = (0 until 5).map { log(date = today.minusDays(it.toLong())) }
        val old = (40 until 45).map { log(date = today.minusDays(it.toLong())) }

        val recentScore = useCase.invoke(
            logs = recent, schedule = HabitSchedule.Daily, today = today,
            createdAtEpochDay = createdAtEpochDay,
        ).strengthScore
        val oldScore = useCase.invoke(
            logs = old, schedule = HabitSchedule.Daily, today = today,
            createdAtEpochDay = createdAtEpochDay,
        ).strengthScore

        assertThat(recentScore).isGreaterThan(oldScore)
    }
}
