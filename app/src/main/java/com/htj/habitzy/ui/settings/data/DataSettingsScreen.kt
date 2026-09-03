package com.htj.habitzy.ui.settings.data

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htj.habitzy.data.backup.CsvColumnMapping
import com.htj.habitzy.data.backup.ImportStrategy
import com.htj.habitzy.data.backup.ImportSummary
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.theme.SpaceL
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataSettingsViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val event = viewModel.events.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var importedSummary by remember { mutableStateOf<ImportSummary?>(null) }
    var showImportStrategy by remember { mutableStateOf<Uri?>(null) }
    var showCsvMapping by remember { mutableStateOf<Uri?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearPhrase by remember { mutableStateOf("") }

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        when (e) {
            is BackupEvent.Exported -> scope.launch { snackbarHostState.showSnackbar(e.message) }
            is BackupEvent.Error -> scope.launch { snackbarHostState.showSnackbar(e.message) }
            is BackupEvent.ImportedCsv -> scope.launch { snackbarHostState.showSnackbar("CSV imported") }
            is BackupEvent.Imported -> importedSummary = e.summary
        }
        viewModel.consumeEvent()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) showImportStrategy = uri
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) showCsvMapping = uri
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setAutoBackupFolder(uri.toString())
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Data") },
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
            SectionHeader(title = "Backup")
            ListItem(
                headlineContent = { Text("Export backup") },
                supportingContent = { Text("Save all data to a JSON file") },
                trailingContent = {
                    TextButton(onClick = { exportLauncher.launch("habitzy-backup.json") }) {
                        Text("Export")
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Import backup") },
                supportingContent = { Text("Restore from a Habitzy JSON file") },
                trailingContent = {
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text("Import")
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Import from CSV") },
                supportingContent = { Text("Import habits with a name and date columns") },
                trailingContent = {
                    TextButton(onClick = { csvLauncher.launch(arrayOf("text/*", "text/csv")) }) {
                        Text("Import")
                    }
                },
            )

            SectionHeader(title = "Automatic backups")
            ListItem(
                headlineContent = { Text("Scheduled backups") },
                supportingContent = { Text("Automatically back up on a schedule") },
                trailingContent = {
                    Switch(
                        checked = uiState.autoBackupEnabled,
                        onCheckedChange = viewModel::setAutoBackupEnabled,
                    )
                },
            )
            if (uiState.autoBackupEnabled) {
                ListItem(
                    headlineContent = { Text("Backup folder") },
                    supportingContent = { Text(uiState.backupFolderUri?.let { "Folder selected" } ?: "Choose a folder to save backups") },
                    trailingContent = {
                        TextButton(onClick = { folderLauncher.launch(null) }) {
                            Text(if (uiState.backupFolderUri == null) "Choose" else "Change")
                        }
                    },
                )
                if (uiState.backupFolderUri == null) {
                    Text(
                        text = "Select a folder before enabling scheduled backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = SpaceL),
                    )
                }
                ListItem(
                    headlineContent = { Text("Frequency") },
                    supportingContent = { Text(if (uiState.autoBackupFrequencyDays == 1) "Daily" else "Every ${uiState.autoBackupFrequencyDays} days") },
                    trailingContent = {
                        SingleChoiceSegmentedButtonRow {
                            listOf(1 to "Daily", 7 to "Weekly").forEachIndexed { index, (days, label) ->
                                SegmentedButton(
                                    selected = uiState.autoBackupFrequencyDays == days,
                                    onClick = { viewModel.setAutoBackupFrequencyDays(days) },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    },
                )
            }

            SectionHeader(title = "Archived habits")
            if (uiState.archivedHabits.isEmpty()) {
                Text(
                    text = "No archived habits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = SpaceL),
                )
            } else {
                uiState.archivedHabits.forEach { habit ->
                    ListItem(
                        headlineContent = { Text(habit.name) },
                        supportingContent = { Text(habit.schedule.summaryText()) },
                        trailingContent = {
                            Column {
                                TextButton(onClick = { viewModel.unarchiveHabit(habit.id) }) {
                                    Text("Unarchive")
                                }
                                TextButton(onClick = { viewModel.deleteHabitPermanently(habit.id) }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                    )
                }
            }

            SectionHeader(title = "Danger zone")
            ListItem(
                headlineContent = { Text("Clear all data") },
                supportingContent = { Text("Permanently delete all habits, logs, notes, and reminders") },
                trailingContent = {
                    TextButton(
                        onClick = { showClearConfirm = true; clearPhrase = "" },
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
            )
            Spacer(Modifier.height(SpaceL))
        }
    }

    importedSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { importedSummary = null },
            title = { Text("Import complete") },
            text = {
                Text("Imported ${summary.habits} habits, ${summary.logs} logs, ${summary.notes} notes, ${summary.reminders} reminders.")
            },
            confirmButton = {
                TextButton(onClick = { importedSummary = null }) { Text("OK") }
            },
        )
    }

    showImportStrategy?.let { uri ->
        val strategy = remember { mutableStateOf(ImportStrategy.MERGE) }
        AlertDialog(
            onDismissRequest = { showImportStrategy = null },
            title = { Text("Import backup") },
            text = {
                Column {
                    Text("Found a Habitzy backup. Choose how to import:")
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow {
                        listOf(ImportStrategy.MERGE to "Merge", ImportStrategy.REPLACE_ALL to "Replace all").forEachIndexed { index, (s, label) ->
                            SegmentedButton(
                                selected = strategy.value == s,
                                onClick = { strategy.value = s },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                            ) {
                                Text(label)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (strategy.value == ImportStrategy.REPLACE_ALL) {
                        Text("This will replace all existing data.", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importFrom(uri, strategy.value)
                        showImportStrategy = null
                    },
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportStrategy = null }) { Text("Cancel") }
            },
        )
    }

    showCsvMapping?.let { uri ->
        CsvMappingDialog(
            onConfirm = { mapping ->
                viewModel.importCsv(uri, mapping)
                showCsvMapping = null
            },
            onDismiss = { showCsvMapping = null },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all data?") },
            text = {
                Column {
                    Text("This permanently deletes all data. Type \"confirm\" to proceed.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clearPhrase,
                        onValueChange = { clearPhrase = it },
                        label = { Text("confirm") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = clearPhrase == "confirm",
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                ) {
                    Text("Clear all data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CsvMappingDialog(
    onConfirm: (CsvColumnMapping) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameColumn by remember { mutableStateOf(0) }
    var dateColumn by remember { mutableStateOf(1) }
    var completedColumn by remember { mutableStateOf<Int?>(null) }
    var hasCompletedColumn by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose CSV columns") },
        text = {
            Column {
                ColumnMapperRow(label = "Habit name column", value = nameColumn, onChange = { nameColumn = it })
                Spacer(Modifier.height(8.dp))
                ColumnMapperRow(label = "Date column", value = dateColumn, onChange = { dateColumn = it })
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Has a completed column") },
                    trailingContent = {
                        Switch(
                            checked = hasCompletedColumn,
                            onCheckedChange = {
                                hasCompletedColumn = it
                                completedColumn = if (it) 2 else null
                            },
                        )
                    },
                )
                if (hasCompletedColumn) {
                    ColumnMapperRow(label = "Completed column", value = completedColumn ?: 2, onChange = { completedColumn = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        CsvColumnMapping(
                            nameColumn = nameColumn,
                            dateColumn = dateColumn,
                            completedColumn = completedColumn,
                        )
                    )
                },
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColumnMapperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(0, 1, 2, 3, 4)
    OutlinedButton(onClick = { expanded = true }) {
        Text("$label: column $value")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { i ->
            DropdownMenuItem(
                text = { Text("Column $i") },
                onClick = {
                    onChange(i)
                    expanded = false
                },
            )
        }
    }
}
