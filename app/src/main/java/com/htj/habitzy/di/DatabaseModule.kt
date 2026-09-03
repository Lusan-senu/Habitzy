package com.htj.habitzy.di

import android.content.Context
import androidx.room.Room
import com.htj.habitzy.data.local.db.HabitzyDatabase
import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.HabitLogDao
import com.htj.habitzy.data.local.db.dao.HabitNoteDao
import com.htj.habitzy.data.local.db.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitzyDatabase =
        Room.databaseBuilder(context, HabitzyDatabase::class.java, "habitzy.db")
            .build()

    @Provides
    fun provideHabitDao(db: HabitzyDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitLogDao(db: HabitzyDatabase): HabitLogDao = db.habitLogDao()

    @Provides
    fun provideHabitNoteDao(db: HabitzyDatabase): HabitNoteDao = db.habitNoteDao()

    @Provides
    fun provideReminderDao(db: HabitzyDatabase): ReminderDao = db.reminderDao()
}
