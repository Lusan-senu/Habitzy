package com.htj.habitzy.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {
    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :day")
    suspend fun getLog(habitId: Long, day: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay BETWEEN :start AND :end ORDER BY epochDay ASC")
    fun observeLogsInRange(habitId: Long, start: Long, end: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE epochDay BETWEEN :start AND :end")
    fun observeAllLogsInRange(start: Long, end: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY epochDay ASC")
    suspend fun getAllLogs(habitId: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs ORDER BY habitId ASC, epochDay ASC")
    suspend fun getAll(): List<HabitLogEntity>

    @Query("DELETE FROM habit_logs")
    suspend fun clearAll()

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :day")
    suspend fun deleteLog(habitId: Long, day: Long)
}
