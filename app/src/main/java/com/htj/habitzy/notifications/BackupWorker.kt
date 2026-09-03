package com.htj.habitzy.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.htj.habitzy.domain.repository.BackupRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val folderUri = settingsRepository.backupFolderUri.first()
        if (folderUri.isNullOrBlank()) return Result.success()

        val fileName = "habitzy_backup_${LocalDate.now()}.json"
        return try {
            backupRepository.exportToFolder(android.net.Uri.parse(folderUri), fileName)
                .fold(
                    onSuccess = { Result.success() },
                    onFailure = { Result.retry() },
                )
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
