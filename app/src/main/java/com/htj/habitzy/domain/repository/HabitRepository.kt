package com.htj.habitzy.domain.repository

import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeArchivedHabits(): Flow<List<Habit>>
    fun observeHabit(id: Long): Flow<Habit?>
    suspend fun getUsedCategoryTags(): List<String>
    suspend fun createHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun archiveHabit(id: Long, archived: Boolean)
    suspend fun deleteHabitPermanently(id: Long)
    suspend fun reorderHabits(orderedIds: List<Long>)
    suspend fun toggleCompletion(habitId: Long, date: LocalDate)
    suspend fun setAmountProgress(habitId: Long, date: LocalDate, value: Double)
    suspend fun setChecklistStep(habitId: Long, date: LocalDate, stepIndex: Int, done: Boolean)
    fun observeLogs(habitId: Long, range: ClosedRange<LocalDate>): Flow<List<HabitLog>>
    fun observeAllLogsInRange(range: ClosedRange<LocalDate>): Flow<List<HabitLog>>
    suspend fun setDayNote(habitId: Long, date: LocalDate, text: String, photoUri: String?)
}
