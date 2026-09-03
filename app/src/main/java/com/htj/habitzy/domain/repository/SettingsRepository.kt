package com.htj.habitzy.domain.repository

import com.htj.habitzy.data.local.datastore.AppPreferences
import com.htj.habitzy.data.local.datastore.ProfilePreferences
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.domain.model.Profile
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val useDynamicColor: Flow<Boolean>
    val accentSeedColor: Flow<Int>
    val useTrueBlack: Flow<Boolean>
    val weekStartDay: Flow<DayOfWeek>
    val defaultReminderHour: Flow<Int>
    val defaultReminderMinute: Flow<Int>
    val autoBackupEnabled: Flow<Boolean>
    val autoBackupFrequencyDays: Flow<Int>
    val appLockEnabled: Flow<Boolean>
    val hapticsEnabled: Flow<Boolean>
    val notificationActions: Flow<Boolean>
    val lastCelebratedEpochDay: Flow<Long?>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(enabled: Boolean)
    suspend fun setAccentSeedColor(argb: Int)
    suspend fun setUseTrueBlack(enabled: Boolean)
    suspend fun setWeekStartDay(day: DayOfWeek)
    suspend fun setDefaultReminderHour(hour: Int)
    suspend fun setDefaultReminderMinute(minute: Int)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setAutoBackupFrequencyDays(days: Int)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setNotificationActions(enabled: Boolean)
    suspend fun setLastCelebratedEpochDay(day: Long)
}

interface ProfileRepository {
    fun observeProfile(): Flow<Profile>
    suspend fun setName(name: String?)
    suspend fun setPhotoUri(uri: String?)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    private val profilePreferences: ProfilePreferences,
) : SettingsRepository, ProfileRepository {
    override val themeMode by appPreferences::themeMode
    override val useDynamicColor by appPreferences::useDynamicColor
    override val accentSeedColor by appPreferences::accentSeedColor
    override val useTrueBlack by appPreferences::useTrueBlack
    override val weekStartDay by appPreferences::weekStartDay
    override val defaultReminderHour by appPreferences::defaultReminderHour
    override val defaultReminderMinute by appPreferences::defaultReminderMinute
    override val autoBackupEnabled by appPreferences::autoBackupEnabled
    override val autoBackupFrequencyDays by appPreferences::autoBackupFrequencyDays
    override val appLockEnabled by appPreferences::appLockEnabled
    override val hapticsEnabled by appPreferences::hapticsEnabled
    override val notificationActions by appPreferences::notificationActions
    override val lastCelebratedEpochDay by appPreferences::lastCelebratedEpochDay

    override suspend fun setThemeMode(mode: ThemeMode) = appPreferences.setThemeMode(mode)
    override suspend fun setUseDynamicColor(enabled: Boolean) = appPreferences.setUseDynamicColor(enabled)
    override suspend fun setAccentSeedColor(argb: Int) = appPreferences.setAccentSeedColor(argb)
    override suspend fun setUseTrueBlack(enabled: Boolean) = appPreferences.setUseTrueBlack(enabled)
    override suspend fun setWeekStartDay(day: DayOfWeek) = appPreferences.setWeekStartDay(day)
    override suspend fun setDefaultReminderHour(hour: Int) = appPreferences.setDefaultReminderHour(hour)
    override suspend fun setDefaultReminderMinute(minute: Int) = appPreferences.setDefaultReminderMinute(minute)
    override suspend fun setAutoBackupEnabled(enabled: Boolean) = appPreferences.setAutoBackupEnabled(enabled)
    override suspend fun setAutoBackupFrequencyDays(days: Int) = appPreferences.setAutoBackupFrequencyDays(days)
    override suspend fun setAppLockEnabled(enabled: Boolean) = appPreferences.setAppLockEnabled(enabled)
    override suspend fun setHapticsEnabled(enabled: Boolean) = appPreferences.setHapticsEnabled(enabled)
    override suspend fun setNotificationActions(enabled: Boolean) = appPreferences.setNotificationActions(enabled)
    override suspend fun setLastCelebratedEpochDay(day: Long) = appPreferences.setLastCelebratedEpochDay(day)

    override fun observeProfile(): Flow<Profile> =
        combine(profilePreferences.name, profilePreferences.photoUri) { name, uri -> Profile(name, uri) }

    override suspend fun setName(name: String?) = profilePreferences.setName(name)
    override suspend fun setPhotoUri(uri: String?) = profilePreferences.setPhotoUri(uri)
}
