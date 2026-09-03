package com.htj.habitzy.ui.habitdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.htj.habitzy.ui.theme.SpaceM
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationPickerDialog(
    initialRange: ClosedRange<LocalDate>?,
    onApply: (LocalDate, LocalDate) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.start?.let { toMillis(it) },
        initialSelectedEndDateMillis = initialRange?.endInclusive?.let { toMillis(it) },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = state.selectedStartDateMillis?.let { toLocalDate(it) }
                    val end = state.selectedEndDateMillis?.let { toLocalDate(it) }
                    if (start != null && end != null) onApply(start, end)
                },
            ) { Text(text = "Apply") }
        },
        dismissButton = {
            Row {
                if (initialRange != null) {
                    TextButton(onClick = onClear) { Text(text = "Clear") }
                }
                TextButton(onClick = onDismiss) { Text(text = "Cancel") }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceM),
        ) {
            Text(
                text = "Select vacation dates",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = SpaceM),
            )
            DateRangePicker(state = state, showModeToggle = false)
        }
    }
}

private fun toMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun toLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
