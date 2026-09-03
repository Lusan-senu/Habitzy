package com.htj.habitzy.ui.addedithabit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.htj.habitzy.ui.components.ColorSwatchPicker
import com.htj.habitzy.ui.components.IconOrEmojiPicker
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LocalTime.of(state.hour, state.minute))
                },
            ) { Text(text = "OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
        title = { Text(text = "Pick a time") },
        text = {
            TimePicker(state = state)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    selectedEmoji: String?,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Choose an icon",
            modifier = Modifier.padding(horizontal = SpaceL),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        )
        IconOrEmojiPicker(
            selectedEmoji = selectedEmoji,
            onEmojiSelected = {
                onSelected(it)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceL),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    selectedColor: Color,
    onSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Choose a color",
            modifier = Modifier.padding(horizontal = SpaceL),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        )
        ColorSwatchPicker(
            selectedColor = selectedColor,
            onColorSelected = {
                onSelected(it)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceL),
        )
        Text(
            text = "Custom…",
            modifier = Modifier.padding(horizontal = SpaceL, vertical = SpaceM),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        CustomColorPicker(
            initialColor = selectedColor,
            onColorChanged = { onSelected(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceL),
        )
    }
}
