package com.hisabnikash.app.backup.local

import android.net.Uri
import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.backup.RestoreRecoveryStore
import com.hisabnikash.app.backup.NoOpRestoreRecoveryStore
import com.hisabnikash.app.data.preferences.LocalBackupPreferences
import kotlinx.coroutines.flow.first
import com.hisabnikash.app.backup.BackupOperationGate

class LocalBackupStore(
    private val documentAccess: LocalDocumentAccess,
    private val preferences: LocalBackupPreferences,
    private val exporter: SnapshotExporter,
    private val importer: SnapshotImporter,
    private val passwordCache: BackupPasswordCache,
    private val recoveryStore: RestoreRecoveryStore = NoOpRestoreRecoveryStore,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val operationGate: BackupOperationGate = BackupOperationGate(),
) {
    suspend fun chooseBackupLocation(uri: Uri) = operationGate.run {
        if (!documentAccess.persistTreeAccess(uri)) throw BackupLocationUnavailableException()
        preferences.setLocalBackup(enabled = true, uri = uri.toString())
    }

    suspend fun disable() = operationGate.run {
        operationGate.invalidateAuthorization()
        preferences.setLocalBackup(enabled = false, uri = null)
        passwordCache.clear()
        preferences.setLastLocalBackupAt(null)
    }

    suspend fun backUpNow(password: CharArray) = operationGate.run { writeBackup(password) }

    suspend fun backUpQueued() = operationGate.run {
        if (!preferences.state.first().localBackupEnabled) return@run
        val password = passwordCache.load() ?: return@run
        try { writeBackup(password) } finally { password.fill('\u0000') }
    }

    private suspend fun writeBackup(password: CharArray) {
        val settings = preferences.state.first()
        if (!settings.localBackupEnabled) throw BackupNotEnabledException()
        val tree = settings.localBackupUri?.let(Uri::parse) ?: throw BackupLocationUnavailableException()
        val encrypted = importer.encode(exporter.export(), password)
        try {
            documentAccess.writeAtomically(tree, FILE_NAME, encrypted)
            passwordCache.save(password)
            preferences.setLastLocalBackupAt(nowEpochMillis())
        } finally {
            encrypted.fill(0)
        }
    }

    suspend fun restoreFromDocument(uri: Uri, password: CharArray): RestoreResult = operationGate.run {
        val encrypted = documentAccess.read(uri)
        try {
            val validated = importer.validate(encrypted, password)
            importer.apply(validated) { recoveryStore.save(password) }
        } finally {
            encrypted.fill(0)
        }
    }

    companion object {
        const val FILE_NAME = "hisab-backup.hisab"
    }
}
