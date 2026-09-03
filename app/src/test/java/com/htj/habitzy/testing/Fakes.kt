package com.htj.habitzy.testing

import com.htj.habitzy.data.local.datastore.AppIcon
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.domain.model.DayNote
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.Reminder
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake for ViewModel tests — mirrors the Room-backed behavior. */
class FakeHabitRepository : HabitRepository {

    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val logs = MutableStateFlow<List<HabitLog>>(emptyList())
    val notes = MutableStateFlow<List<DayNote>>(emptyList())
    val reminders = MutableStateFlow<List<Reminder>>(emptyList())

    var nextId = 1L
    var nextSortOrder = 0

    override fun observeActiveHabits(): Flow<List<Habit>> = habits.map { list -> list.filter { !it.isArchived } }
    override fun observeArchivedHabits(): Flow<List<Habit>> = habits.map { list -> list.filter { it.isArchived } }
    override fun observeHabit(id: Long): Flow<Habit?> = habits.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getUsedCategoryTags(): List<String> =
        habits.value.mapNotNull { it.categoryTag }.distinct().sorted()

    override suspend fun getNextSortOrder(): Int = nextSortOrder

    override suspend fun createHabit(habit: Habit): Long {
        val id = nextId++
        habits.value = habits.value + habit.copy(id = id)
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        habits.value = habits.value.map { if (it.id == habit.id) habit else it }
    }

    override suspend fun archiveHabit(id: Long, archived: Boolean) {
        habits.value = habits.value.map {
            if (it.id == id) it.copy(isArchived = archived) else it
        }
    }

    override suspend fun deleteHabitPermanently(id: Long) {
        habits.value = habits.value.filter { it.id != id }
        logs.value = logs.value.filter { it.habitId != id }
    }

    override suspend fun reorderHabits(orderedIds: List<Long>) {
        habits.value = habits.value.mapIndexed { index, habit ->
            orderedIds.indexOf(habit.id).let { position ->
                if (position >= 0) habit.copy(sortOrder = position) else habit
            }
        }
    }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate) {
        val existing = logs.value.firstOrNull { it.habitId == habitId && it.date == date }
        if (existing == null) {
            logs.value = logs.value + HabitLog(
                habitId = habitId,
                date = date,
                isCompleted = true,
                amountValue = null,
                checklistDoneMask = 0,
                completedAt = null,
            )
        } else {
            logs.value = logs.value.map {
                if (it === existing) it.copy(isCompleted = !it.isCompleted) else it
            }
        }
    }

    override suspend fun setAmountProgress(habitId: Long, date: LocalDate, value: Double) {
        val existing = logs.value.firstOrNull { it.habitId == habitId && it.date == date }
        if (existing == null) {
            logs.value = logs.value + HabitLog(
                habitId = habitId,
                date = date,
                isCompleted = false,
                amountValue = value,
                checklistDoneMask = 0,
                completedAt = null,
            )
        } else {
            logs.value = logs.value.map {
                if (it === existing) it.copy(amountValue = value, isCompleted = value > 0.0) else it
            }
        }
    }

    override suspend fun setChecklistStep(habitId: Long, date: LocalDate, stepIndex: Int, done: Boolean) {
        val existing = logs.value.firstOrNull { it.habitId == habitId && it.date == date }
        val currentMask = existing?.checklistDoneMask ?: 0
        val newMask = if (done) currentMask or (1 shl stepIndex) else currentMask and (1 shl stepIndex).inv()
        val base = existing ?: HabitLog(
            habitId = habitId,
            date = date,
            isCompleted = false,
            amountValue = null,
            checklistDoneMask = 0,
            completedAt = null,
        )
        logs.value = logs.value.filterNot { it.habitId == habitId && it.date == date } +
            base.copy(checklistDoneMask = newMask)
    }

    override fun observeLogs(habitId: Long, range: ClosedRange<LocalDate>): Flow<List<HabitLog>> =
        logs.map { list -> list.filter { it.habitId == habitId && it.date in range } }

    override fun observeAllLogsInRange(range: ClosedRange<LocalDate>): Flow<List<HabitLog>> =
        logs.map { list -> list.filter { it.date in range } }

    override fun observeNotes(habitId: Long): Flow<List<DayNote>> =
        notes.map { list -> list.filter { it.habitId == habitId } }

    override suspend fun setDayNote(habitId: Long, date: LocalDate, text: String, photoUri: String?) {
        val existing = notes.value.firstOrNull { it.habitId == habitId && it.date == date }
        if (existing == null) {
            notes.value = notes.value + DayNote(id = 0, habitId, date, text, photoUri)
        } else {
            notes.value = notes.value.map {
                if (it === existing) it.copy(text = text, photoUri = photoUri) else it
            }
        }
    }

    override fun observeReminders(habitId: Long): Flow<List<Reminder>> =
        reminders.map { list -> list.filter { it.habitId == habitId } }

    override suspend fun saveReminders(habitId: Long, reminders: List<Reminder>) {
        this.reminders.value = this.reminders.value.filter { it.habitId != habitId } +
            reminders.map { it.copy(habitId = habitId) }
    }
}

/** In-memory fake of the DataStore-backed settings repository. */
class FakeSettingsRepository : SettingsRepository {
    val prefs = MutableStateFlow(mapOf<String, Any?>())

    private inline fun <reified T : Any> get(key: String, default: T): T =
        prefs.value[key] as? T ?: default

    private suspend fun set(key: String, value: Any?) {
        prefs.value = prefs.value + (key to value)
    }

    override val themeMode: Flow<ThemeMode> = prefs.map { it["theme"] as? ThemeMode ?: ThemeMode.SYSTEM }
    override val useDynamicColor: Flow<Boolean> = prefs.map { it["dynamic"] as? Boolean ?: false }
    override val accentSeedColor: Flow<Int> = prefs.map { it["seed"] as? Int ?: 0xFFFF5A36.toInt() }
    override val useTrueBlack: Flow<Boolean> = prefs.map { it["black"] as? Boolean ?: false }
    override val weekStartDay: Flow<DayOfWeek> = prefs.map { it["weekStart"] as? DayOfWeek ?: DayOfWeek.MONDAY }
    override val defaultReminderHour: Flow<Int> = prefs.map { it["remHour"] as? Int ?: 9 }
    override val defaultReminderMinute: Flow<Int> = prefs.map { it["remMin"] as? Int ?: 0 }
    override val autoBackupEnabled: Flow<Boolean> = prefs.map { it["backupOn"] as? Boolean ?: false }
    override val autoBackupFrequencyDays: Flow<Int> = prefs.map { it["backupFreq"] as? Int ?: 7 }
    override val backupFolderUri: Flow<String?> = prefs.map { it["backupUri"] as? String }
    override val appLockEnabled: Flow<Boolean> = prefs.map { it["lock"] as? Boolean ?: false }
    override val appLockPin: Flow<String?> = prefs.map { it["pin"] as? String }
    override val hapticsEnabled: Flow<Boolean> = prefs.map { it["haptics"] as? Boolean ?: true }
    override val notificationActions: Flow<Boolean> = prefs.map { it["notifActions"] as? Boolean ?: true }
    override val lastCelebratedEpochDay: Flow<Long?> = prefs.map { it["celebrated"] as? Long }
    override val appIcon: Flow<AppIcon> = prefs.map { it["appIcon"] as? AppIcon ?: AppIcon.DEFAULT }

    override suspend fun setThemeMode(mode: ThemeMode) = set("theme", mode)
    override suspend fun setUseDynamicColor(enabled: Boolean) = set("dynamic", enabled)
    override suspend fun setAccentSeedColor(argb: Int) = set("seed", argb)
    override suspend fun setUseTrueBlack(enabled: Boolean) = set("black", enabled)
    override suspend fun setWeekStartDay(day: DayOfWeek) = set("weekStart", day)
    override suspend fun setDefaultReminderHour(hour: Int) = set("remHour", hour)
    override suspend fun setDefaultReminderMinute(minute: Int) = set("remMin", minute)
    override suspend fun setAutoBackupEnabled(enabled: Boolean) = set("backupOn", enabled)
    override suspend fun setAutoBackupFrequencyDays(days: Int) = set("backupFreq", days)
    override suspend fun setBackupFolderUri(uri: String?) = set("backupUri", uri)
    override suspend fun setAppLockEnabled(enabled: Boolean) = set("lock", enabled)
    override suspend fun setAppLockPin(pin: String?) = set("pin", pin)
    override suspend fun setHapticsEnabled(enabled: Boolean) = set("haptics", enabled)
    override suspend fun setNotificationActions(enabled: Boolean) = set("notifActions", enabled)
    override suspend fun setLastCelebratedEpochDay(day: Long) = set("celebrated", day)
    override suspend fun setAppIcon(icon: AppIcon) = set("appIcon", icon)
}
