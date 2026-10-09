package com.hisabnikash.app.backup.drive

import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.RestoreRecoveryStore
import com.hisabnikash.app.backup.NoOpRestoreRecoveryStore
import com.hisabnikash.app.backup.BackupOperationGate
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
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
    private val operationGate: BackupOperationGate = BackupOperationGate(),
    private val isEnabled: suspend () -> Boolean = { true },
    private val setEnabled: suspend (Boolean) -> Unit = {},
    private val passwordCache: BackupPasswordCache? = null,
    private val setBackupReady: suspend (Boolean) -> Unit = {},
) {
    private val mutableStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = mutableStatus

    val authorizationVersion: Long get() = operationGate.version

    suspend fun connect(expectedVersion: Long): Boolean = operationGate.run {
        if (expectedVersion != operationGate.version) return@run false
        setEnabled(true)
        true
    }

    suspend fun disconnect(revoke: suspend () -> Unit) = operationGate.run {
        operationGate.invalidateAuthorization()
        setEnabled(false)
        passwordCache?.clear()
        setBackupReady(false)
        mutableStatus.value = BackupStatus.Idle
        revoke()
    }

    suspend fun backUpNow(accessToken: String, password: CharArray) = operationGate.run {
        if (!isEnabled()) throw DriveBackupDisabledException()
        writeBackup(accessToken, password)
    }

    suspend fun backUpQueued(authorize: suspend () -> String?) = operationGate.run {
        if (!isEnabled()) return@run
        val password = passwordCache?.load() ?: run {
            setBackupReady(false)
            throw DriveReauthorizationRequired()
        }
        try {
            val token = authorize() ?: throw DriveReauthorizationRequired()
            writeBackup(token, password)
        } finally { password.fill('\u0000') }
    }

    private suspend fun writeBackup(accessToken: String, password: CharArray) {
        mutableStatus.value = BackupStatus.Running
        var encrypted: ByteArray? = null
        try {
            val envelope = importer.encode(exporter.export(), password)
            encrypted = envelope
            remote.uploadLatest(accessToken, envelope)
            passwordCache?.save(password)
            setBackupReady(true)
            mutableStatus.value = BackupStatus.Succeeded(nowEpochMillis())
        } catch (error: Throwable) {
            mutableStatus.value = BackupStatus.Failed(DriveBackupWorkerPolicy.shouldRetry(error))
            throw error
        } finally {
            encrypted?.fill(0)
        }
    }

    suspend fun restoreLatest(accessToken: String, password: CharArray): RestoreResult = operationGate.run {
        if (!isEnabled()) throw DriveBackupDisabledException()
        val encrypted = remote.downloadLatest(accessToken) ?: throw DriveException("No Drive backup found")
        try {
            val validated = importer.validate(encrypted, password)
            importer.apply(validated) { recoveryStore.save(password) }
        } finally {
            encrypted.fill(0)
        }
    }
}

class DriveBackupDisabledException : DriveException("Drive backup is disabled")
