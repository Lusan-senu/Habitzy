package com.htj.habitzy.domain.repository

import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.DayNote
import com.htj.habitzy.domain.model.Reminder
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeArchivedHabits(): Flow<List<Habit>>
    fun observeHabit(id: Long): Flow<Habit?>
    suspend fun getUsedCategoryTags(): List<String>
    suspend fun getNextSortOrder(): Int
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
    fun observeNotes(habitId: Long): Flow<List<DayNote>>
    suspend fun setDayNote(habitId: Long, date: LocalDate, text: String, photoUri: String?)
    fun observeReminders(habitId: Long): Flow<List<Reminder>>
    suspend fun saveReminders(habitId: Long, reminders: List<Reminder>)
}
