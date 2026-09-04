package com.htj.habitzy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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
): ColorScheme {
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

/**
 * App-wide theme wrapper.
 *
 * Phase 1 of the Material 3 Expressive migration: wired through
 * [MaterialExpressiveTheme] instead of the plain MaterialTheme so every child
 * composable picks up expressive defaults (LocalMotionScheme, expressive shape
 * defaults on stock components, etc). [motionScheme] defaults to the stock
 * expressive scheme for now — Phase 5 replaces the hand-rolled springs in
 * Motion.kt with specs derived from this same scheme instead of a second,
 * disconnected motion system.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitzyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    seedColor: Color = HabitzyDefaultSeed,
    trueBlack: Boolean = false,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    content: @Composable () -> Unit,
) {
    val colorScheme = habitzyColorScheme(seedColor, darkTheme, dynamicColor, trueBlack)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        typography = HabitzyTypography,
        shapes = HabitzyShapes,
        content = content,
    )
}