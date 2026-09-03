package com.htj.habitzy.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class HabitzyBackup(
    val formatVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val habits: List<BackupHabit>,
    val logs: List<BackupLog>,
    val notes: List<BackupNote>,
    val reminders: List<BackupReminder>,
    val appPreferences: BackupPreferences? = null,
)

@Serializable
data class BackupPreferences(
    val themeMode: String? = null,
    val dynamicColor: Boolean? = null,
    val accentSeedArgb: Int? = null,
    val trueBlack: Boolean? = null,
    val weekStartDay: String? = null,
)

@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val description: String? = null,
    val iconKey: String,
    val colorSeedArgb: Int,
    val type: String,
    val amountGoal: Double? = null,
    val amountUnit: String? = null,
    val checklistItemsJson: String? = null,
    val scheduleType: String,
    val scheduleWeekdaysMask: Int = 0,
    val scheduleTimesTarget: Int = 0,
    val scheduleEveryNDays: Int = 0,
    val categoryTag: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochDay: Long,
    val isArchived: Boolean = false,
    val vacationStartEpochDay: Long? = null,
    val vacationEndEpochDay: Long? = null,
)

@Serializable
data class BackupLog(
    val habitId: Long,
    val epochDay: Long,
    val isCompleted: Boolean,
    val amountValue: Double? = null,
    val checklistDoneMask: Int = 0,
    val completedAtEpochMillis: Long? = null,
)

@Serializable
data class BackupNote(
    val habitId: Long,
    val epochDay: Long,
    val text: String,
    val photoUri: String? = null,
)

@Serializable
data class BackupReminder(
    val habitId: Long,
    val hour: Int,
    val minute: Int,
    val message: String? = null,
    val isEnabled: Boolean = true,
)

enum class ImportStrategy { MERGE, REPLACE_ALL }

data class ImportSummary(
    val habits: Int,
    val logs: Int,
    val notes: Int,
    val reminders: Int,
)

data class CsvColumnMapping(
    val nameColumn: Int,
    val dateColumn: Int,
    val completedColumn: Int? = null,
    val hasHeader: Boolean = true,
)
