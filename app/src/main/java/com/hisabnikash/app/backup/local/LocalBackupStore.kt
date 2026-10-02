package com.hisabnikash.app.backup.local

import android.net.Uri
import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.data.preferences.LocalBackupPreferences
import kotlinx.coroutines.flow.first

class LocalBackupStore(
    private val documentAccess: LocalDocumentAccess,
    private val preferences: LocalBackupPreferences,
    private val exporter: SnapshotExporter,
    private val importer: SnapshotImporter,
    private val passwordCache: BackupPasswordCache,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun chooseBackupLocation(uri: Uri) {
        if (!documentAccess.persistTreeAccess(uri)) throw BackupLocationUnavailableException()
        preferences.setLocalBackup(enabled = true, uri = uri.toString())
    }

    suspend fun disable() {
        preferences.setLocalBackup(enabled = false, uri = null)
        preferences.setLastLocalBackupAt(null)
        passwordCache.clear()
    }

    suspend fun backUpNow(password: CharArray) {
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

    suspend fun restoreFromDocument(uri: Uri, password: CharArray): RestoreResult {
        val encrypted = documentAccess.read(uri)
        return try {
            val validated = importer.validate(encrypted, password)
            importer.apply(validated)
        } finally {
            encrypted.fill(0)
        }
    }

    companion object {
        const val FILE_NAME = "hisab-backup.hisab"
    }
}
