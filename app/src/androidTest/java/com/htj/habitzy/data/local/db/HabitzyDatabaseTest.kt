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

    @Test
    fun `range queries return only logs within the epochDay window`() = runBlocking {
        val secondId = db.habitDao().insert(
            HabitEntity(
                name = "Read",
                iconKey = "emoji:📖",
                colorSeedArgb = 0xFF00A896.toInt(),
                type = "BINARY",
                scheduleType = "DAILY",
                createdAtEpochDay = 20000L,
            )
        )
        // habit: 20001..20003, second: 20003..20004
        for (day in 20001L..20003L) {
            db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = day, isCompleted = true))
        }
        for (day in 20003L..20004L) {
            db.habitLogDao().upsert(HabitLogEntity(habitId = secondId, epochDay = day, isCompleted = true))
        }

        val inRange = db.habitLogDao().observeLogsInRange(habit.id, 20001L, 20002L).first()
        assertEquals(listOf(20001L, 20002L), inRange.map { it.epochDay })

        val allInRange = db.habitLogDao().observeAllLogsInRange(20002L, 20003L).first()
        assertEquals(3, allInRange.size)
        assertTrue(allInRange.all { it.epochDay in 20002L..20003L })

        val secondLogs = db.habitLogDao().getAllLogs(secondId)
        assertEquals(listOf(20003L, 20004L), secondLogs.map { it.epochDay })
    }

    @Test
    fun `deleting habit cascades to notes and reminders`() = runBlocking {
        db.habitNoteDao().insert(
            com.htj.habitzy.data.local.db.entity.HabitNoteEntity(
                habitId = habit.id, epochDay = 20001L, text = "note",
            )
        )
        db.reminderDao().upsert(
            com.htj.habitzy.data.local.db.entity.ReminderEntity(
                habitId = habit.id, hour = 8, minute = 0,
            )
        )

        assertEquals(1, db.habitNoteDao().getAll().size)
        assertEquals(1, db.reminderDao().getAll().size)

        db.habitDao().delete(habit)

        assertEquals(0, db.habitNoteDao().getAll().size)
        assertEquals(0, db.reminderDao().getAll().size)
    }

    @Test
    fun `deleteLog removes a single day without touching others`() = runBlocking {
        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = 20001L, isCompleted = true))
        db.habitLogDao().upsert(HabitLogEntity(habitId = habit.id, epochDay = 20002L, isCompleted = true))

        db.habitLogDao().deleteLog(habit.id, 20001L)

        val logs = db.habitLogDao().getAllLogs(habit.id)
        assertEquals(listOf(20002L), logs.map { it.epochDay })
    }
}
