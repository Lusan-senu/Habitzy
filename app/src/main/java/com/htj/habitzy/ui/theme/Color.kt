package com.htj.habitzy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

val HabitzyDefaultSeed = Color(0xFFFF5A36)

/**
 * Per-habit accent color swatches (curated, accessible-contrast palette).
 *
 * These are SEED colors only — never paint one of these directly as a fill or
 * background. Run it through [rememberHabitAccentColors] first so habit accents
 * inherit real expressive tonal relationships (container/on-container) instead
 * of a flat hex fill with an alpha hack. See HabitCard.kt / StreakBadge.kt /
 * ProgressRing.kt (Phase 7) for the call-site swap.
 */
val HabitSwatch1 = Color(0xFFFF5A36) // coral
val HabitSwatch2 = Color(0xFF00A896) // teal
val HabitSwatch3 = Color(0xFF6C5CE7) // purple
val HabitSwatch4 = Color(0xFF0984E3) // blue
val HabitSwatch5 = Color(0xFF00B894) // green
val HabitSwatch6 = Color(0xFFE17055) // terra cotta
val HabitSwatch7 = Color(0xFFFDCB6E) // gold
val HabitSwatch8 = Color(0xFFE84393) // pink
val HabitSwatch9 = Color(0xFF00CEC9) // cyan
val HabitSwatch10 = Color(0xFF6C5CE7) // violet
val HabitSwatch11 = Color(0xFFD63031) // red
val HabitSwatch12 = Color(0xFF00B894) // mint

/** A tonal container pair derived from a habit's seed color. */
data class HabitAccentColors(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
)

/**
 * Derives an expressive tonal container/on-container pair from a habit's seed
 * color — the same mechanism [habitzyColorScheme] uses for the app's own accent
 * seed. Use this anywhere a habit's raw [Color] would otherwise be painted
 * directly, so habit-colored surfaces get a real container relationship instead
 * of `color.copy(alpha = ...)`.
 */
@Composable
fun rememberHabitAccentColors(
    seedColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
): HabitAccentColors {
    val scheme: ColorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.Expressive,
    )
    return HabitAccentColors(
        container = scheme.primaryContainer,
        onContainer = scheme.onPrimaryContainer,
        accent = scheme.primary,
    )
}