package com.htj.habitzy.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.htj.habitzy.data.local.db.converter.Converters
import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.HabitLogDao
import com.htj.habitzy.data.local.db.dao.HabitNoteDao
import com.htj.habitzy.data.local.db.dao.ReminderDao
import com.htj.habitzy.data.local.db.entity.HabitEntity
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import com.htj.habitzy.data.local.db.entity.HabitNoteEntity
import com.htj.habitzy.data.local.db.entity.ReminderEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        HabitNoteEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HabitzyDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun habitNoteDao(): HabitNoteDao
    abstract fun reminderDao(): ReminderDao
}
