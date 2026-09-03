package com.htj.habitzy.data.repository

import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.HabitLogDao
import com.htj.habitzy.domain.model.InsightSummary
import com.htj.habitzy.domain.model.StreakInfo
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.InsightsRepository
import com.htj.habitzy.domain.usecase.ComputeInsightsUseCase
import com.htj.habitzy.domain.usecase.ComputeStreakUseCase
import com.htj.habitzy.domain.usecase.InsightPeriod
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class InsightsRepositoryImpl @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitLogDao: HabitLogDao,
    private val computeStreak: ComputeStreakUseCase,
    private val computeInsights: ComputeInsightsUseCase,
) : InsightsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStreakInfo(habitId: Long): Flow<StreakInfo> {
        return habitRepository.observeHabit(habitId).flatMapLatest { habit ->
            if (habit == null) {
                kotlinx.coroutines.flow.flowOf(
                    StreakInfo(0, 0, 0, 0f, 0f, 0f, 0f, null)
                )
            } else {
                habitLogDao.observeLogsInRange(habitId, LocalDate.MIN.toEpochDay(), LocalDate.MAX.toEpochDay())
                    .map { logs ->
                        computeStreak.invoke(
                            logs = logs.map { entity ->
                                com.htj.habitzy.domain.model.HabitLog(
                                    habitId = entity.habitId,
                                    date = LocalDate.ofEpochDay(entity.epochDay),
                                    isCompleted = entity.isCompleted,
                                    amountValue = entity.amountValue,
                                    checklistDoneMask = entity.checklistDoneMask,
                                    completedAt = entity.completedAtEpochMillis?.let(java.time.Instant::ofEpochMilli),
                                )
                            },
                            schedule = habit.schedule,
                            today = LocalDate.now(),
                            vacationRange = habit.vacationRange,
                        )
                    }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeGlobalInsights(period: InsightPeriod): Flow<InsightSummary> {
        return combine(
            habitRepository.observeActiveHabits(),
            habitLogDao.observeAllLogsInRange(LocalDate.MIN.toEpochDay(), LocalDate.MAX.toEpochDay()),
        ) { habits, entities ->
            val logs = entities.map { entity ->
                com.htj.habitzy.domain.model.HabitLog(
                    habitId = entity.habitId,
                    date = LocalDate.ofEpochDay(entity.epochDay),
                    isCompleted = entity.isCompleted,
                    amountValue = entity.amountValue,
                    checklistDoneMask = entity.checklistDoneMask,
                    completedAt = entity.completedAtEpochMillis?.let(java.time.Instant::ofEpochMilli),
                )
            }
            computeInsights.invoke(habits, logs, LocalDate.now(), period)
        }
    }
}
