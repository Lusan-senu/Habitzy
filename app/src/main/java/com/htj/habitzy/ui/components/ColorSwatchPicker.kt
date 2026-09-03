package com.htj.habitzy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htj.habitzy.ui.theme.HabitSwatch1
import com.htj.habitzy.ui.theme.HabitSwatch10
import com.htj.habitzy.ui.theme.HabitSwatch11
import com.htj.habitzy.ui.theme.HabitSwatch12
import com.htj.habitzy.ui.theme.HabitSwatch2
import com.htj.habitzy.ui.theme.HabitSwatch3
import com.htj.habitzy.ui.theme.HabitSwatch4
import com.htj.habitzy.ui.theme.HabitSwatch5
import com.htj.habitzy.ui.theme.HabitSwatch6
import com.htj.habitzy.ui.theme.HabitSwatch7
import com.htj.habitzy.ui.theme.HabitSwatch8
import com.htj.habitzy.ui.theme.HabitSwatch9
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceS

val HabitSwatches = listOf(
    HabitSwatch1, HabitSwatch2, HabitSwatch3, HabitSwatch4,
    HabitSwatch5, HabitSwatch6, HabitSwatch7, HabitSwatch8,
    HabitSwatch9, HabitSwatch10, HabitSwatch11, HabitSwatch12,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SpaceS),
        verticalArrangement = Arrangement.spacedBy(SpaceS),
    ) {
        HabitSwatches.forEach { swatch ->
            val isSelected = swatch == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ShapeFull)
                    .background(swatch)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, ShapeFull)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelected(swatch) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
