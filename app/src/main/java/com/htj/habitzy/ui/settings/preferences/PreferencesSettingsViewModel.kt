package com.htj.habitzy.ui.settings.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreferencesUiState(
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val hapticsEnabled: Boolean = true,
    val notificationActions: Boolean = true,
)

@HiltViewModel
class PreferencesSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<PreferencesUiState> = combine(
        settingsRepository.weekStartDay,
        settingsRepository.defaultReminderHour,
        settingsRepository.defaultReminderMinute,
        settingsRepository.hapticsEnabled,
        settingsRepository.notificationActions,
    ) { weekStart, hour, minute, haptics, actions ->
        PreferencesUiState(
            weekStart = weekStart,
            defaultReminderHour = hour,
            defaultReminderMinute = minute,
            hapticsEnabled = haptics,
            notificationActions = actions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreferencesUiState(),
    )

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { settingsRepository.setWeekStartDay(day) }
    }

    fun setDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultReminderHour(hour)
            settingsRepository.setDefaultReminderMinute(minute)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setNotificationActions(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationActions(enabled) }
    }
}
