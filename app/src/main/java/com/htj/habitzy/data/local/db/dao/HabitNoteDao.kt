package com.htj.habitzy.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.htj.habitzy.data.local.db.entity.HabitNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitNoteDao {
    @Insert
    suspend fun insert(note: HabitNoteEntity): Long

    @Query("SELECT * FROM habit_notes WHERE habitId = :habitId ORDER BY epochDay DESC")
    fun observeNotes(habitId: Long): Flow<List<HabitNoteEntity>>

    @Query("SELECT * FROM habit_notes WHERE habitId = :habitId AND epochDay = :day")
    suspend fun getNote(habitId: Long, day: Long): HabitNoteEntity?

    @Query("DELETE FROM habit_notes WHERE habitId = :habitId AND epochDay = :day")
    suspend fun deleteNote(habitId: Long, day: Long)
}
