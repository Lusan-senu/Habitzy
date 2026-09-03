package com.htj.habitzy.domain.repository

import android.net.Uri
import com.htj.habitzy.data.backup.CsvColumnMapping
import com.htj.habitzy.data.backup.ImportStrategy
import com.htj.habitzy.data.backup.ImportSummary

interface BackupRepository {
    suspend fun exportToUri(uri: Uri): Result<Unit>
    suspend fun exportToFolder(treeUri: Uri, fileName: String): Result<Unit>
    suspend fun importFromUri(uri: Uri, strategy: ImportStrategy): Result<ImportSummary>
    suspend fun importCsv(uri: Uri, mapping: CsvColumnMapping): Result<ImportSummary>
    suspend fun clearAllData(): Result<Unit>
}
