package com.hisabnikash.app.backup.drive

import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.RestoreRecoveryStore
import com.hisabnikash.app.backup.NoOpRestoreRecoveryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Running : BackupStatus
    data class Succeeded(val atEpochMillis: Long) : BackupStatus
    data class Failed(val retryable: Boolean) : BackupStatus
}

class DriveBackupStore(
    private val remote: DriveRemoteStore,
    private val exporter: SnapshotExporter,
    private val importer: SnapshotImporter,
    private val recoveryStore: RestoreRecoveryStore = NoOpRestoreRecoveryStore,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutableStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = mutableStatus

    suspend fun backUpNow(accessToken: String, password: CharArray) {
        mutableStatus.value = BackupStatus.Running
        val encrypted = importer.encode(exporter.export(), password)
        try {
            remote.uploadLatest(accessToken, encrypted)
            mutableStatus.value = BackupStatus.Succeeded(nowEpochMillis())
        } catch (error: Throwable) {
            mutableStatus.value = BackupStatus.Failed(DriveBackupWorkerPolicy.shouldRetry(error))
            throw error
        } finally {
            encrypted.fill(0)
        }
    }

    suspend fun restoreLatest(accessToken: String, password: CharArray): RestoreResult {
        val encrypted = remote.downloadLatest(accessToken) ?: throw DriveException("No Drive backup found")
        return try {
            val validated = importer.validate(encrypted, password)
            recoveryStore.save(password)
            importer.apply(validated)
        } finally {
            encrypted.fill(0)
        }
    }
}
