package com.htj.habitzy.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.htj.habitzy.data.backup.BackupHabit
import com.htj.habitzy.data.backup.BackupLog
import com.htj.habitzy.data.backup.BackupNote
import com.htj.habitzy.data.backup.BackupPreferences
import com.htj.habitzy.data.backup.BackupReminder
import com.htj.habitzy.data.backup.CsvColumnMapping
import com.htj.habitzy.data.backup.CsvParser
import com.htj.habitzy.data.backup.HabitzyBackup
import com.htj.habitzy.data.backup.ImportStrategy
import com.htj.habitzy.data.backup.ImportSummary
import com.htj.habitzy.data.local.datastore.AppPreferences
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.HabitLogDao
import com.htj.habitzy.data.local.db.dao.HabitNoteDao
import com.htj.habitzy.data.local.db.dao.ReminderDao
import com.htj.habitzy.data.local.db.entity.HabitEntity
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import com.htj.habitzy.data.local.db.entity.HabitNoteEntity
import com.htj.habitzy.data.local.db.entity.ReminderEntity
import com.htj.habitzy.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val noteDao: HabitNoteDao,
    private val reminderDao: ReminderDao,
    private val appPreferences: AppPreferences,
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val habits = habitDao.getAll()
            val logs = logDao.getAll()
            val notes = noteDao.getAll()
            val reminders = reminderDao.getAll()

            val backup = HabitzyBackup(
                exportedAtEpochMillis = System.currentTimeMillis(),
                habits = habits.map(::toBackupHabit),
                logs = logs.map(::toBackupLog),
                notes = notes.map(::toBackupNote),
                reminders = reminders.map(::toBackupReminder),
                appPreferences = toBackupPreferences(),
            )
            val content = json.encodeToString(HabitzyBackup.serializer(), backup)
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: error("Could not open output stream")
        }
    }

    override suspend fun exportToFolder(treeUri: Uri, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val docUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    treeUri,
                    "application/json",
                    fileName,
                ) ?: error("Could not create backup document")
                exportToUri(docUri).getOrThrow()
            }
        }

    override suspend fun importFromUri(uri: Uri, strategy: ImportStrategy): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not open input stream")
                val backup = json.decodeFromString(HabitzyBackup.serializer(), content.decodeToString())

                if (strategy == ImportStrategy.REPLACE_ALL) {
                    clearAllTables()
                }

                val habitIdMap = HashMap<Long, Long>()
                var habitCount = 0
                var logCount = 0
                var noteCount = 0
                var reminderCount = 0

                backup.habits.forEach { bh ->
                    val newId = habitDao.insert(toEntity(bh))
                    habitIdMap[bh.id] = newId
                    habitCount++
                }
                backup.logs.forEach { bl ->
                    val newHabitId = habitIdMap[bl.habitId] ?: return@forEach
                    logDao.upsert(
                        HabitLogEntity(
                            habitId = newHabitId,
                            epochDay = bl.epochDay,
                            isCompleted = bl.isCompleted,
                            amountValue = bl.amountValue,
                            checklistDoneMask = bl.checklistDoneMask,
                            completedAtEpochMillis = bl.completedAtEpochMillis,
                        )
                    )
                    logCount++
                }
                backup.notes.forEach { bn ->
                    val newHabitId = habitIdMap[bn.habitId] ?: return@forEach
                    noteDao.insert(
                        HabitNoteEntity(
                            habitId = newHabitId,
                            epochDay = bn.epochDay,
                            text = bn.text,
                            photoUri = bn.photoUri,
                        )
                    )
                    noteCount++
                }
                backup.reminders.forEach { br ->
                    val newHabitId = habitIdMap[br.habitId] ?: return@forEach
                    reminderDao.upsert(
                        ReminderEntity(
                            habitId = newHabitId,
                            hour = br.hour,
                            minute = br.minute,
                            message = br.message,
                            isEnabled = br.isEnabled,
                        )
                    )
                    reminderCount++
                }

                if (strategy == ImportStrategy.REPLACE_ALL) {
                    applyPreferences(backup.appPreferences)
                }

                ImportSummary(habitCount, logCount, noteCount, reminderCount)
            }
        }

    override suspend fun importCsv(uri: Uri, mapping: CsvColumnMapping): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not open input stream")
                val lines = content.decodeToString()
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()

                if (lines.isEmpty()) return@runCatching ImportSummary(0, 0, 0, 0)

                val startIndex = if (mapping.hasHeader) 1 else 0
                var habitCount = 0
                var logCount = 0

                for (i in startIndex until lines.size) {
                    val cols = CsvParser.splitLine(lines[i])
                    if (cols.size <= maxOf(mapping.nameColumn, mapping.dateColumn)) continue
                    val name = cols.getOrNull(mapping.nameColumn)?.trim() ?: continue
                    if (name.isEmpty()) continue
                    val dateStr = cols.getOrNull(mapping.dateColumn)?.trim() ?: continue
                    val date = CsvParser.parseDate(dateStr) ?: continue
                    val isCompleted = mapping.completedColumn?.let {
                        CsvParser.parseBool(cols.getOrNull(it)) ?: true
                    } ?: true

                    val existing = habitDao.getAll().firstOrNull {
                        it.name.equals(name, ignoreCase = true) && it.type == "BINARY"
                    }
                    val habitId = if (existing != null) {
                        existing.id
                    } else {
                        val id = habitDao.insert(
                            HabitEntity(
                                name = name,
                                iconKey = "emoji:⭐",
                                colorSeedArgb = 0xFFFF5A36.toInt(),
                                type = "BINARY",
                                scheduleType = "DAILY",
                                createdAtEpochDay = LocalDate.now().toEpochDay(),
                            )
                        )
                        habitCount++
                        id
                    }

                    val existingLog = logDao.getLog(habitId, date.toEpochDay())
                    if (existingLog == null) {
                        logDao.upsert(
                            HabitLogEntity(
                                habitId = habitId,
                                epochDay = date.toEpochDay(),
                                isCompleted = isCompleted,
                                completedAtEpochMillis = if (isCompleted) System.currentTimeMillis() else null,
                            )
                        )
                        logCount++
                    }
                }

                ImportSummary(habitCount, logCount, 0, 0)
            }
        }

    override suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { clearAllTables() }
    }

    private suspend fun clearAllTables() {
        logDao.clearAll()
        noteDao.clearAll()
        reminderDao.clearAll()
        habitDao.clearAll()
    }

    private fun toBackupHabit(e: HabitEntity) = BackupHabit(
        id = e.id,
        name = e.name,
        description = e.description,
        iconKey = e.iconKey,
        colorSeedArgb = e.colorSeedArgb,
        type = e.type,
        amountGoal = e.amountGoal,
        amountUnit = e.amountUnit,
        checklistItemsJson = e.checklistItemsJson,
        scheduleType = e.scheduleType,
        scheduleWeekdaysMask = e.scheduleWeekdaysMask,
        scheduleTimesTarget = e.scheduleTimesTarget,
        scheduleEveryNDays = e.scheduleEveryNDays,
        categoryTag = e.categoryTag,
        sortOrder = e.sortOrder,
        createdAtEpochDay = e.createdAtEpochDay,
        isArchived = e.isArchived,
        vacationStartEpochDay = e.vacationStartEpochDay,
        vacationEndEpochDay = e.vacationEndEpochDay,
    )

    private fun toBackupLog(e: HabitLogEntity) = BackupLog(
        habitId = e.habitId,
        epochDay = e.epochDay,
        isCompleted = e.isCompleted,
        amountValue = e.amountValue,
        checklistDoneMask = e.checklistDoneMask,
        completedAtEpochMillis = e.completedAtEpochMillis,
    )

    private fun toBackupNote(e: HabitNoteEntity) = BackupNote(
        habitId = e.habitId,
        epochDay = e.epochDay,
        text = e.text,
        photoUri = e.photoUri,
    )

    private fun toBackupReminder(e: ReminderEntity) = BackupReminder(
        habitId = e.habitId,
        hour = e.hour,
        minute = e.minute,
        message = e.message,
        isEnabled = e.isEnabled,
    )

    private suspend fun toBackupPreferences(): BackupPreferences = BackupPreferences(
        themeMode = appPreferences.themeMode.first().name,
        dynamicColor = appPreferences.useDynamicColor.first(),
        accentSeedArgb = appPreferences.accentSeedColor.first(),
        trueBlack = appPreferences.useTrueBlack.first(),
        weekStartDay = appPreferences.weekStartDay.first().name,
    )

    private suspend fun applyPreferences(p: BackupPreferences?) {
        if (p == null) return
        p.themeMode?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()?.let { appPreferences.setThemeMode(it) }
        }
        p.dynamicColor?.let { appPreferences.setUseDynamicColor(it) }
        p.accentSeedArgb?.let { appPreferences.setAccentSeedColor(it) }
        p.trueBlack?.let { appPreferences.setUseTrueBlack(it) }
        p.weekStartDay?.let { name ->
            runCatching { java.time.DayOfWeek.valueOf(name) }.getOrNull()?.let { appPreferences.setWeekStartDay(it) }
        }
    }

    private fun toEntity(b: BackupHabit) = HabitEntity(
        id = 0,
        name = b.name,
        description = b.description,
        iconKey = b.iconKey,
        colorSeedArgb = b.colorSeedArgb,
        type = b.type,
        amountGoal = b.amountGoal,
        amountUnit = b.amountUnit,
        checklistItemsJson = b.checklistItemsJson,
        scheduleType = b.scheduleType,
        scheduleWeekdaysMask = b.scheduleWeekdaysMask,
        scheduleTimesTarget = b.scheduleTimesTarget,
        scheduleEveryNDays = b.scheduleEveryNDays,
        categoryTag = b.categoryTag,
        sortOrder = b.sortOrder,
        createdAtEpochDay = b.createdAtEpochDay,
        isArchived = b.isArchived,
        vacationStartEpochDay = b.vacationStartEpochDay,
        vacationEndEpochDay = b.vacationEndEpochDay,
    )
}
