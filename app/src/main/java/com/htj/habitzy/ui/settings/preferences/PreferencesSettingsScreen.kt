package com.htj.habitzy.ui.settings.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.htj.habitzy.ui.addedithabit.ReminderTimePickerDialog
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.theme.SpaceL
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val WeekStartOptions = listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun DayOfWeek.label(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreferencesSettingsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = "Week starts on")
            Row(
                modifier = Modifier.padding(horizontal = SpaceL),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeekStartOptions.forEach { day ->
                    FilterChip(
                        selected = uiState.weekStart == day,
                        onClick = { viewModel.setWeekStart(day) },
                        label = { Text(day.label()) },
                    )
                }
            }

            SectionHeader(title = "Reminders")
            ListItem(
                headlineContent = { Text("Default reminder time") },
                supportingContent = { Text("%02d:%02d".format(uiState.defaultReminderHour, uiState.defaultReminderMinute)) },
                trailingContent = {
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("Change")
                    }
                },
            )

            SectionHeader(title = "Feedback")
            ListItem(
                headlineContent = { Text("Haptics") },
                supportingContent = { Text("Haptic feedback when completing a habit") },
                trailingContent = {
                    Switch(
                        checked = uiState.hapticsEnabled,
                        onCheckedChange = viewModel::setHapticsEnabled,
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Notification actions") },
                supportingContent = { Text("Show Done / Snooze buttons on reminders") },
                trailingContent = {
                    Switch(
                        checked = uiState.notificationActions,
                        onCheckedChange = viewModel::setNotificationActions,
                    )
                },
            )
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = uiState.defaultReminderHour,
            initialMinute = uiState.defaultReminderMinute,
            onConfirm = {
                viewModel.setDefaultReminderTime(it.hour, it.minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}
