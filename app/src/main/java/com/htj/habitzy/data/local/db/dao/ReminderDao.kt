package com.htj.habitzy.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.htj.habitzy.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE habitId = :habitId ORDER BY hour ASC, minute ASC")
    fun observeReminders(habitId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE habitId = :habitId")
    suspend fun getRemindersForHabit(habitId: Long): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: Long)
}
