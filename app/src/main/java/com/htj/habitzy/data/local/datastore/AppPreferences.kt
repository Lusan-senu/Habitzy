package com.htj.habitzy.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.htj.habitzy.ui.theme.HabitzyDefaultSeed
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habitzy_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_SEED = intPreferencesKey("accent_seed_argb")
        val TRUE_BLACK = booleanPreferencesKey("true_black")
        val WEEK_START = stringPreferencesKey("week_start_day")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY_DAYS = intPreferencesKey("auto_backup_frequency_days")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val LAST_CELEBRATED_EPOCH_DAY = longPreferencesKey("last_celebrated_epoch_day")
        val NOTIFICATION_ACTIONS = booleanPreferencesKey("notification_actions")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        ThemeMode.valueOf(it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    suspend fun setUseDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    val accentSeedColor: Flow<Int> = context.dataStore.data.map {
        it[Keys.ACCENT_SEED] ?: HabitzyDefaultSeed.toArgb()
    }
    suspend fun setAccentSeedColor(argb: Int) = edit { it[Keys.ACCENT_SEED] = argb }

    val useTrueBlack: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRUE_BLACK] ?: false }
    suspend fun setUseTrueBlack(enabled: Boolean) = edit { it[Keys.TRUE_BLACK] = enabled }

    val weekStartDay: Flow<DayOfWeek> = context.dataStore.data.map {
        DayOfWeek.valueOf(it[Keys.WEEK_START] ?: DayOfWeek.MONDAY.name)
    }
    suspend fun setWeekStartDay(day: DayOfWeek) = edit { it[Keys.WEEK_START] = day.name }

    val defaultReminderHour: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_REMINDER_HOUR] ?: 9 }
    suspend fun setDefaultReminderHour(hour: Int) = edit { it[Keys.DEFAULT_REMINDER_HOUR] = hour }

    val defaultReminderMinute: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_REMINDER_MINUTE] ?: 0 }
    suspend fun setDefaultReminderMinute(minute: Int) = edit { it[Keys.DEFAULT_REMINDER_MINUTE] = minute }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: false }
    suspend fun setAutoBackupEnabled(enabled: Boolean) = edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }

    val autoBackupFrequencyDays: Flow<Int> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_FREQUENCY_DAYS] ?: 7 }
    suspend fun setAutoBackupFrequencyDays(days: Int) = edit { it[Keys.AUTO_BACKUP_FREQUENCY_DAYS] = days }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }
    suspend fun setAppLockEnabled(enabled: Boolean) = edit { it[Keys.APP_LOCK_ENABLED] = enabled }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }
    suspend fun setHapticsEnabled(enabled: Boolean) = edit { it[Keys.HAPTICS_ENABLED] = enabled }

    val lastCelebratedEpochDay: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_CELEBRATED_EPOCH_DAY] }
    suspend fun setLastCelebratedEpochDay(day: Long) = edit { it[Keys.LAST_CELEBRATED_EPOCH_DAY] = day }

    val notificationActions: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATION_ACTIONS] ?: true }
    suspend fun setNotificationActions(enabled: Boolean) = edit { it[Keys.NOTIFICATION_ACTIONS] = enabled }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
