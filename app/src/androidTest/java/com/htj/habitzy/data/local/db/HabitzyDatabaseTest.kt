package com.htj.habitzy.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.htj.habitzy.data.local.db.entity.HabitEntity
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitzyDatabaseTest {

    private lateinit var db: HabitzyDatabase
    private lateinit var habit: HabitEntity

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitzyDatabase::class.java,
        ).allowMainThreadQueries().build()

        runBlocking {
            val habitId = db.habitDao().insert(
                HabitEntity(
                    name = "Drink water",
                    iconKey = "emoji:💧",
                    colorSeedArgb = 0xFF00A896.toInt(),
                    type = "BINARY",
                    scheduleType = "DAILY",
                    createdAtEpochDay = 20000L,
                )
            )
            habit = db.habitDao().getHabit(habitId)!!
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `unique index upsert keeps one row per day`() = runBlocking {
        val day = 20001L

        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = day, isCompleted = true))
        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = day, isCompleted = false))

        val logs = db.habitLogDao().getAllLogs(habit.id)
        assertEquals(1, logs.size)
        assertTrue(!logs[0].isCompleted)
    }

    @Test
    fun `deleting habit cascades to its logs`() = runBlocking {
        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = 20001L, isCompleted = true))
        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = 20002L, isCompleted = true))

        assertEquals(2, db.habitLogDao().getAllLogs(habit.id).size)

        db.habitDao().delete(habit)

        assertEquals(0, db.habitLogDao().getAllLogs(habit.id).size)
    }
}
