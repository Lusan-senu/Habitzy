package com.htj.habitzy.ui.settings.appearance

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.data.local.datastore.AppIcon
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val accentSeedArgb: Int = 0,
    val useTrueBlack: Boolean = false,
    val appIcon: AppIcon = AppIcon.DEFAULT,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AppearanceUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.useDynamicColor,
        settingsRepository.accentSeedColor,
        settingsRepository.useTrueBlack,
        settingsRepository.appIcon,
    ) { theme, dynamic, accent, trueBlack, appIcon ->
        AppearanceUiState(
            themeMode = theme,
            useDynamicColor = dynamic,
            accentSeedArgb = accent,
            useTrueBlack = trueBlack,
            appIcon = appIcon,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDynamicColor(enabled) }
    }

    fun setAccentSeed(argb: Int) {
        viewModelScope.launch { settingsRepository.setAccentSeedColor(argb) }
    }

    fun setUseTrueBlack(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseTrueBlack(enabled) }
    }

    fun setAppIcon(icon: AppIcon) {
        viewModelScope.launch {
            settingsRepository.setAppIcon(icon)
            applyLauncherAlias(icon)
        }
    }

    private fun applyLauncherAlias(selected: AppIcon) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        AppIcon.entries.forEach { icon ->
            val component = ComponentName(packageName, "$packageName.${icon.componentSuffix}")
            val state = if (icon == selected) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
