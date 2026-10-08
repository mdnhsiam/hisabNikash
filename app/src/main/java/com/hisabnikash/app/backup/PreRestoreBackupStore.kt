package com.hisabnikash.app.backup

import android.content.Context
import java.io.File
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
) : RestoreRecoveryStore {
    private val file = File(context.filesDir, FILE_NAME)

    override suspend fun save(password: CharArray) = withContext(Dispatchers.IO) {
        val encrypted = importer.encode(exporter.export(), password)
        val temporary = File(file.parentFile, "$FILE_NAME.tmp")
        try {
            temporary.writeBytes(encrypted)
            if (file.exists() && !file.delete()) error("Could not replace recovery backup")
            if (!temporary.renameTo(file)) error("Could not commit recovery backup")
        } finally {
            encrypted.fill(0)
            if (temporary.exists()) temporary.delete()
        }
    }

    override suspend fun restore(password: CharArray): RestoreResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext RestoreResult.Failed(IllegalStateException("No recovery backup"))
        val bytes = file.readBytes()
        try { importer.apply(importer.validate(bytes, password)) } finally { bytes.fill(0) }
    }

    override fun exists(): Boolean = file.exists()
    override fun clear() { file.delete() }

    companion object { private const val FILE_NAME = "pre-restore.hisab" }
}
