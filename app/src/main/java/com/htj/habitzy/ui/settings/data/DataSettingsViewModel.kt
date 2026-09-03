package com.htj.habitzy.ui.settings.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.data.backup.CsvColumnMapping
import com.htj.habitzy.data.backup.ImportStrategy
import com.htj.habitzy.data.backup.ImportSummary
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.repository.BackupRepository
import com.htj.habitzy.domain.repository.HabitRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import com.htj.habitzy.notifications.BackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

data class DataUiState(
    val archivedHabits: List<Habit> = emptyList(),
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequencyDays: Int = 7,
    val backupFolderUri: String? = null,
)

sealed interface BackupEvent {
    data class Exported(val message: String) : BackupEvent
    data class Imported(val summary: ImportSummary) : BackupEvent
    data class Error(val message: String) : BackupEvent
    data object ImportedCsv : BackupEvent
}

@HiltViewModel
class DataSettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val backupScheduler: BackupScheduler,
) : ViewModel() {

    private val _events = MutableStateFlow<BackupEvent?>(null)
    val events: StateFlow<BackupEvent?> = _events.asStateFlow()

    val uiState: StateFlow<DataUiState> = combine(
        habitRepository.observeArchivedHabits(),
        settingsRepository.autoBackupEnabled,
        settingsRepository.autoBackupFrequencyDays,
        settingsRepository.backupFolderUri,
    ) { archived, auto, freq, folder ->
        DataUiState(
            archivedHabits = archived,
            autoBackupEnabled = auto,
            autoBackupFrequencyDays = freq,
            backupFolderUri = folder,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DataUiState(),
    )

    init {
        viewModelScope.launch {
            val enabled = settingsRepository.autoBackupEnabled.first()
            val freq = settingsRepository.autoBackupFrequencyDays.first()
            if (enabled) backupScheduler.schedule(freq) else backupScheduler.cancel()
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportToUri(uri)
                .onSuccess { _events.value = BackupEvent.Exported("Backup exported") }
                .onFailure { _events.value = BackupEvent.Error(it.message ?: "Export failed") }
        }
    }

    fun importFrom(uri: Uri, strategy: ImportStrategy) {
        viewModelScope.launch {
            backupRepository.importFromUri(uri, strategy)
                .onSuccess {
                    _events.value = BackupEvent.Imported(it)
                }
                .onFailure { _events.value = BackupEvent.Error(it.message ?: "Import failed") }
        }
    }

    fun importCsv(uri: Uri, mapping: CsvColumnMapping) {
        viewModelScope.launch {
            backupRepository.importCsv(uri, mapping)
                .onSuccess { _events.value = BackupEvent.ImportedCsv }
                .onFailure { _events.value = BackupEvent.Error(it.message ?: "CSV import failed") }
        }
    }

    fun unarchiveHabit(id: Long) {
        viewModelScope.launch { habitRepository.archiveHabit(id, false) }
    }

    fun deleteHabitPermanently(id: Long) {
        viewModelScope.launch { habitRepository.deleteHabitPermanently(id) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            backupRepository.clearAllData()
                .onSuccess { _events.value = BackupEvent.Exported("All data cleared") }
                .onFailure { _events.value = BackupEvent.Error(it.message ?: "Clear failed") }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                backupScheduler.schedule(settingsRepository.autoBackupFrequencyDays.first())
            } else {
                backupScheduler.cancel()
            }
        }
    }

    fun setAutoBackupFrequencyDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupFrequencyDays(days)
            if (settingsRepository.autoBackupEnabled.first()) {
                backupScheduler.schedule(days)
            }
        }
    }

    fun setAutoBackupFolder(uriString: String?) {
        viewModelScope.launch {
            settingsRepository.setBackupFolderUri(uriString)
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
