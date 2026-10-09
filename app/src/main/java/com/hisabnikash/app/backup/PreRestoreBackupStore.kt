package com.hisabnikash.app.backup

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RestoreRecoveryStore {
    suspend fun save(password: CharArray)
    suspend fun restore(password: CharArray): RestoreResult
    fun exists(): Boolean
    fun clear()
}

object NoOpRestoreRecoveryStore : RestoreRecoveryStore {
    override suspend fun save(password: CharArray) = Unit
    override suspend fun restore(password: CharArray): RestoreResult = RestoreResult.Failed(IllegalStateException("No recovery backup"))
    override fun exists() = false
    override fun clear() = Unit
}

class PreRestoreBackupStore(
    context: Context,
    private val exporter: SnapshotExporter,
    private val importer: SnapshotImporter,
    private val operationGate: BackupOperationGate = BackupOperationGate(),
) : RestoreRecoveryStore {
    private val file = File(context.filesDir, FILE_NAME)
    private val previous = File(context.filesDir, "$FILE_NAME.previous")
    private val temporary = File(context.filesDir, "$FILE_NAME.tmp")

    override suspend fun save(password: CharArray) = withContext(Dispatchers.IO) {
        val encrypted = importer.encode(exporter.export(), password)
        try {
            if (!file.exists() && previous.exists() && !previous.renameTo(file)) error("Could not recover previous backup")
            FileOutputStream(temporary).use { stream -> stream.write(encrypted); stream.fd.sync() }
            if (file.exists()) {
                if (previous.exists() && !previous.delete()) error("Could not replace recovery backup")
                if (!file.renameTo(previous)) error("Could not retain recovery backup")
            }
            if (!temporary.renameTo(file)) {
                if (!file.exists() && previous.exists()) previous.renameTo(file)
                error("Could not commit recovery backup")
            }
            previous.delete()
            Unit
        } finally {
            encrypted.fill(0)
            if (temporary.exists()) temporary.delete()
        }
    }

    override suspend fun restore(password: CharArray): RestoreResult = operationGate.run {
        withContext(Dispatchers.IO) {
            val available = if (file.exists()) file else previous
            if (!available.exists() || available.length() > MAX_BACKUP_BYTES) return@withContext RestoreResult.Failed(IllegalStateException("No recovery backup"))
            val bytes = available.readBytes()
            try { importer.apply(importer.validate(bytes, password)) } finally { bytes.fill(0) }
        }
    }

    override fun exists(): Boolean = file.exists() || previous.exists()
    override fun clear() { file.delete(); previous.delete(); temporary.delete() }

    companion object {
        private const val FILE_NAME = "pre-restore.hisab"
        private const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
    }
}
