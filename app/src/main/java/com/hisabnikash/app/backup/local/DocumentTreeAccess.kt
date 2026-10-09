package com.hisabnikash.app.backup.local

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

interface LocalDocumentAccess {
    fun persistTreeAccess(uri: Uri): Boolean
    fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray)
    fun read(uri: Uri): ByteArray
}

class BackupLocationUnavailableException(cause: Throwable? = null) : Exception("Backup location unavailable", cause)
class BackupNotEnabledException : Exception("Local backup is not enabled")
class BackupPasswordUnavailableException : Exception("Backup password unavailable")

class DocumentTreeAccess(context: Context) : LocalDocumentAccess {
    private val resolver = context.contentResolver

    override fun persistTreeAccess(uri: Uri): Boolean = try {
        resolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        true
    } catch (_: SecurityException) {
        false
    }

    override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) {
        try {
            val oldName = ".$fileName.previous"
            val interruptedPrevious = findChild(treeUri, oldName)
            if (findChild(treeUri, fileName) == null && interruptedPrevious != null) {
                // Preserve the only complete copy after interrupted promotion/rollback.
                DocumentsContract.renameDocument(resolver, interruptedPrevious, fileName)
                    ?: throw BackupLocationUnavailableException()
            }
            val tempName = ".$fileName.tmp"
            findChild(treeUri, tempName)?.let { DocumentsContract.deleteDocument(resolver, it) }
            val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            val temp = DocumentsContract.createDocument(resolver, parent, MIME_TYPE, tempName)
                ?: throw BackupLocationUnavailableException()
            resolver.openOutputStream(temp, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: throw BackupLocationUnavailableException()

            val old = findChild(treeUri, fileName)
            if (old != null) findChild(treeUri, oldName)?.let { DocumentsContract.deleteDocument(resolver, it) }
            val parkedOld = old?.let {
                DocumentsContract.renameDocument(resolver, it, oldName) ?: throw BackupLocationUnavailableException()
            }
            try {
                DocumentsContract.renameDocument(resolver, temp, fileName)
                    ?: throw BackupLocationUnavailableException()
                parkedOld?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
            } catch (error: Throwable) {
                parkedOld?.let { runCatching { DocumentsContract.renameDocument(resolver, it, fileName) } }
                throw error
            }
        } catch (error: BackupLocationUnavailableException) {
            throw error
        } catch (error: Throwable) {
            throw BackupLocationUnavailableException(error)
        }
    }

    override fun read(uri: Uri): ByteArray = try {
        resolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_BACKUP_BYTES) throw BackupLocationUnavailableException()
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw BackupLocationUnavailableException()
    } catch (error: BackupLocationUnavailableException) {
        throw error
    } catch (error: Throwable) {
        throw BackupLocationUnavailableException(error)
    }

    private fun findChild(treeUri: Uri, displayName: String): Uri? {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == displayName) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idIndex))
                }
            }
        }
        return null
    }

    companion object {
        private const val MIME_TYPE = "application/octet-stream"
        private const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
    }
}
