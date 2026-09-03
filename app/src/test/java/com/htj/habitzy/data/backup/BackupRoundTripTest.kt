package com.htj.habitzy.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.data.local.datastore.AppPreferences
import com.htj.habitzy.data.local.db.HabitzyDatabase
import com.htj.habitzy.data.local.db.entity.HabitEntity
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import com.htj.habitzy.data.local.db.entity.HabitNoteEntity
import com.htj.habitzy.data.local.db.entity.ReminderEntity
import com.htj.habitzy.data.repository.BackupRepositoryImpl
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * §12.9: export → wipe → import reproduces every habit, log, note, and reminder.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {

    private lateinit var db: HabitzyDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var context: Context
    private lateinit var backupFile: File

    private val habit = HabitEntity(
        name = "Run 5k",
        description = "Morning run",
        iconKey = "emoji:🏃",
        colorSeedArgb = 0xFFFF5A36.toInt(),
        type = "AMOUNT",
        amountGoal = 5.0,
        amountUnit = "km",
        scheduleType = "SPECIFIC_WEEKDAYS",
        scheduleWeekdaysMask = 0b0010101, // Mon, Wed, Fri
        categoryTag = "health",
        sortOrder = 3,
        createdAtEpochDay = 20000L,
        vacationStartEpochDay = 20100L,
        vacationEndEpochDay = 20105L,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HabitzyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepositoryImpl(
            context = context,
            habitDao = db.habitDao(),
            logDao = db.habitLogDao(),
            noteDao = db.habitNoteDao(),
            reminderDao = db.reminderDao(),
            appPreferences = AppPreferences(context),
        )
        backupFile = File.createTempFile("habitzy_backup", ".json", context.cacheDir)
    }

    @After
    fun tearDown() {
        db.close()
        backupFile.delete()
    }

    private fun uri() = Uri.fromFile(backupFile)

    @Test
    fun `export wipe import reproduces every habit log note and reminder`() = runBlocking {
        // Seed original data.
        val habitId = db.habitDao().insert(habit)
        db.habitLogDao().upsert(
            HabitLogEntity(habitId = habitId, epochDay = 20001L, isCompleted = true, amountValue = 5.0, completedAtEpochMillis = 1770000000000)
        )
        db.habitLogDao().upsert(
            HabitLogEntity(habitId = habitId, epochDay = 20002L, isCompleted = false, amountValue = 2.5)
        )
        db.habitNoteDao().insert(
            HabitNoteEntity(habitId = habitId, epochDay = 20001L, text = "Felt great", photoUri = "content://photo/1")
        )
        db.reminderDao().upsert(
            ReminderEntity(habitId = habitId, hour = 7, minute = 30, message = "Let's go!", isEnabled = true)
        )

        // Export.
        val export = repository.exportToUri(uri())
        assertThat(export.isSuccess).isTrue()

        // Wipe.
        val wipe = repository.clearAllData()
        assertThat(wipe.isSuccess).isTrue()
        assertThat(db.habitDao().getAll()).isEmpty()
        assertThat(db.habitLogDao().getAll()).isEmpty()
        assertThat(db.habitNoteDao().getAll()).isEmpty()
        assertThat(db.reminderDao().getAll()).isEmpty()

        // Import.
        val import = repository.importFromUri(uri(), ImportStrategy.REPLACE_ALL)
        assertThat(import.isSuccess).isTrue()
        val summary = import.getOrThrow()
        assertThat(summary.habits).isEqualTo(1)
        assertThat(summary.logs).isEqualTo(2)
        assertThat(summary.notes).isEqualTo(1)
        assertThat(summary.reminders).isEqualTo(1)

        // Verify every habit field round-trips.
        val restored = db.habitDao().getAll().single()
        assertThat(restored.name).isEqualTo("Run 5k")
        assertThat(restored.description).isEqualTo("Morning run")
        assertThat(restored.iconKey).isEqualTo("emoji:🏃")
        assertThat(restored.colorSeedArgb).isEqualTo(0xFFFF5A36.toInt())
        assertThat(restored.type).isEqualTo("AMOUNT")
        assertThat(restored.amountGoal).isEqualTo(5.0)
        assertThat(restored.amountUnit).isEqualTo("km")
        assertThat(restored.scheduleType).isEqualTo("SPECIFIC_WEEKDAYS")
        assertThat(restored.scheduleWeekdaysMask).isEqualTo(0b0010101)
        assertThat(restored.categoryTag).isEqualTo("health")
        assertThat(restored.sortOrder).isEqualTo(3)
        assertThat(restored.createdAtEpochDay).isEqualTo(20000L)
        assertThat(restored.vacationStartEpochDay).isEqualTo(20100L)
        assertThat(restored.vacationEndEpochDay).isEqualTo(20105L)

        // Logs round-trip with all values.
        val logs = db.habitLogDao().getAllLogs(restored.id)
        assertThat(logs).hasSize(2)
        val log1 = logs.first { it.epochDay == 20001L }
        assertThat(log1.isCompleted).isTrue()
        assertThat(log1.amountValue).isEqualTo(5.0)
        assertThat(log1.completedAtEpochMillis).isEqualTo(1770000000000)
        val log2 = logs.first { it.epochDay == 20002L }
        assertThat(log2.isCompleted).isFalse()
        assertThat(log2.amountValue).isEqualTo(2.5)

        // Notes and reminders round-trip.
        val note = db.habitNoteDao().getAll().single()
        assertThat(note.text).isEqualTo("Felt great")
        assertThat(note.photoUri).isEqualTo("content://photo/1")
        val reminder = db.reminderDao().getAll().single()
        assertThat(reminder.hour).isEqualTo(7)
        assertThat(reminder.minute).isEqualTo(30)
        assertThat(reminder.message).isEqualTo("Let's go!")
        assertThat(reminder.isEnabled).isTrue()
    }

    @Test
    fun `merge import preserves existing habits and appends backup data`() = runBlocking {
        db.habitDao().insert(habit.copy(name = "Existing habit"))
        assertThat(db.habitDao().getAll()).hasSize(1)

        // Export the current single-habit DB, then add one more habit locally.
        val export = repository.exportToUri(uri())
        assertThat(export.isSuccess).isTrue()
        db.habitDao().insert(habit.copy(name = "Second local habit"))
        assertThat(db.habitDao().getAll()).hasSize(2)

        val import = repository.importFromUri(uri(), ImportStrategy.MERGE)
        assertThat(import.isSuccess).isTrue()
        val summary = import.getOrThrow()

        // The backed-up habit was re-appended; the local habit survives untouched.
        assertThat(summary.habits).isEqualTo(1)
        val names = db.habitDao().getAll().map { it.name }
        // "Existing habit" appears twice: the original plus the merged copy from the backup.
        assertThat(names.count { it == "Existing habit" }).isEqualTo(2)
        assertThat(names.count { it == "Second local habit" }).isEqualTo(1)
        Unit
    }

    @Test
    fun `csv import maps habit date and completed columns`() = runBlocking {
        val csv = """
            habit,date,completed
            Running,2026-03-01,yes
            Running,2026-03-02,no
            Reading,2026-03-01,true
        """.trimIndent()
        backupFile.writeText(csv)

        val result = repository.importCsv(
            uri(),
            CsvColumnMapping(nameColumn = 0, dateColumn = 1, completedColumn = 2, hasHeader = true),
        )

        assertThat(result.isSuccess).isTrue()
        val summary = result.getOrThrow()
        assertThat(summary.habits).isEqualTo(2) // Running + Reading created
        assertThat(summary.logs).isEqualTo(3)

        val running = db.habitDao().getAll().first { it.name == "Running" }
        val runningLogs = db.habitLogDao().getAllLogs(running.id)
        assertThat(runningLogs).hasSize(2)
        assertThat(runningLogs.first { it.epochDay == java.time.LocalDate.of(2026, 3, 1).toEpochDay() }.isCompleted).isTrue()
        assertThat(runningLogs.first { it.epochDay == java.time.LocalDate.of(2026, 3, 2).toEpochDay() }.isCompleted).isFalse()
    }
}
