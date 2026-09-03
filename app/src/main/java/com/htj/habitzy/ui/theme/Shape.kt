package com.htj.habitzy.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val HabitzyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Expressive-only extra tokens not part of the base Shapes class
val ShapeLargeIncreased = RoundedCornerShape(20.dp)
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)
val ShapeFull = CircleShape
