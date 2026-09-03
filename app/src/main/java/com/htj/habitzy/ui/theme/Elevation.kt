package com.htj.habitzy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HabitzyElevation(val shadowDp: Dp) {
    Level0(0.dp),
    Level1(1.dp),
    Level2(3.dp),
    Level3(6.dp),
    Level4(8.dp),
    Level5(12.dp),
}

@Composable
fun HabitzyElevation.surfaceColor(scheme: ColorScheme): Color = when (this) {
    HabitzyElevation.Level0 -> scheme.surface
    HabitzyElevation.Level1 -> scheme.surfaceContainerLow
    HabitzyElevation.Level2 -> scheme.surfaceContainer
    HabitzyElevation.Level3 -> scheme.surfaceContainerHigh
    HabitzyElevation.Level4, HabitzyElevation.Level5 -> scheme.surfaceContainerHighest
}
