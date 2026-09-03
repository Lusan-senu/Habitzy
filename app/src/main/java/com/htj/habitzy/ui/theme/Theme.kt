package com.htj.habitzy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun habitzyColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    trueBlack: Boolean,
): androidx.compose.material3.ColorScheme {
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else {
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            isAmoled = trueBlack,
            style = PaletteStyle.Expressive,
        )
    }
    return base
}

@Composable
fun HabitzyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    seedColor: Color = HabitzyDefaultSeed,
    trueBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = habitzyColorScheme(seedColor, darkTheme, dynamicColor, trueBlack)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HabitzyTypography,
        shapes = HabitzyShapes,
        content = content,
    )
}
