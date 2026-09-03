package com.htj.habitzy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.htj.habitzy.data.local.datastore.AppPreferences
import com.htj.habitzy.data.local.datastore.ThemeMode
import com.htj.habitzy.security.AppLockScreen
import com.htj.habitzy.ui.HabitzyApp
import com.htj.habitzy.ui.theme.HabitzyDefaultSeed
import com.htj.habitzy.ui.theme.HabitzyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitzyRootTheme(appPreferences)
        }
    }
}

@Composable
private fun HabitzyRootTheme(appPreferences: AppPreferences) {
    val systemDark = isSystemInDarkTheme()

    val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val useDynamicColor by appPreferences.useDynamicColor.collectAsState(initial = false)
    val accentSeed by appPreferences.accentSeedColor.collectAsState(initial = HabitzyDefaultSeed.toArgb())
    val useTrueBlack by appPreferences.useTrueBlack.collectAsState(initial = false)

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    HabitzyTheme(
        darkTheme = darkTheme,
        dynamicColor = useDynamicColor,
        seedColor = Color(accentSeed),
        trueBlack = useTrueBlack,
    ) {
        AppLockGate(appPreferences) {
            HabitzyApp()
        }
    }
}

@Composable
private fun AppLockGate(
    appPreferences: AppPreferences,
    content: @Composable () -> Unit,
) {
    val enabled by appPreferences.appLockEnabled.collectAsState(initial = false)
    val pinHash by appPreferences.appLockPin.collectAsState(initial = null)
    var unlocked by remember { mutableStateOf(!enabled) }

    val storedHash = pinHash
    if (enabled && storedHash != null && !unlocked) {
        AppLockScreen(
            storedPinHash = storedHash,
            onUnlocked = { unlocked = true },
        )
    } else {
        content()
    }
}
