package com.hisabnikash.app.backup.local

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class DocumentTreeAccessTest {
    @Test fun `retry after interrupted promotion never deletes last good previous snapshot`() {
        val info = android.content.pm.ProviderInfo().apply {
            authority = "backup.fixture"
            exported = true
            grantUriPermissions = true
            readPermission = android.Manifest.permission.MANAGE_DOCUMENTS
            writePermission = android.Manifest.permission.MANAGE_DOCUMENTS
        }
        val provider = Robolectric.buildContentProvider(FixtureDocumentsProvider::class.java).create(info).get()
        provider.add(LocalBackupStore.FILE_NAME, byteArrayOf(1, 2, 3))
        provider.failPromotion = true
        val access = DocumentTreeAccess(ApplicationProvider.getApplicationContext<Context>())
        val tree = DocumentsContract.buildTreeDocumentUri("backup.fixture", "root")

        val firstFailure = runCatching { access.writeAtomically(tree, LocalBackupStore.FILE_NAME, byteArrayOf(4)) }.exceptionOrNull()
        assertTrue("first promotion and rollback must fail", firstFailure != null)
        assertTrue("provider must reach the intended promotion failure: $firstFailure / ${firstFailure?.cause}", provider.promotionAttempts > 0)
        assertArrayEquals(byteArrayOf(1, 2, 3), provider.contents(".${LocalBackupStore.FILE_NAME}.previous"))
        assertTrue(runCatching { access.writeAtomically(tree, LocalBackupStore.FILE_NAME, byteArrayOf(5)) }.isFailure)
        assertArrayEquals(byteArrayOf(1, 2, 3), provider.contents(".${LocalBackupStore.FILE_NAME}.previous"))

        provider.failPromotion = false
        access.writeAtomically(tree, LocalBackupStore.FILE_NAME, byteArrayOf(6))
        assertArrayEquals(byteArrayOf(6), provider.contents(LocalBackupStore.FILE_NAME))
    }
}

class FixtureDocumentsProvider : DocumentsProvider() {
    private val names = linkedMapOf<String, String>()
    private val files = linkedMapOf<String, File>()
    var failPromotion = false
    var promotionAttempts = 0
    override fun onCreate() = true
    fun add(name: String, bytes: ByteArray): String {
        val id = "fixture-${files.size}"
        names[id] = name
        files[id] = File.createTempFile("saf-fixture-", ".bin", requireNotNull(context).cacheDir).apply { writeBytes(bytes) }
        return id
    }
    fun contents(name: String): ByteArray? = names.entries.find { it.value == name }?.key?.let { files.getValue(it).readBytes() }
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = cursor().apply { names[documentId]?.let { addRow(arrayOf(documentId, it)) } }
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = cursor().apply {
        names.forEach { (id, name) -> addRow(arrayOf(id, name)) }
    }
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String = add(displayName, byteArrayOf())
    override fun renameDocument(documentId: String, displayName: String): String? {
        if (displayName == LocalBackupStore.FILE_NAME) promotionAttempts++
        if (failPromotion && displayName == LocalBackupStore.FILE_NAME) throw java.io.FileNotFoundException("fixture promotion failed")
        names[documentId] = displayName
        return documentId
    }
    override fun deleteDocument(documentId: String) { names.remove(documentId); files.remove(documentId)?.delete() }
    override fun isChildDocument(parentDocumentId: String, documentId: String) = parentDocumentId == "root" && documentId in names
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor =
        ParcelFileDescriptor.open(files.getValue(documentId), ParcelFileDescriptor.parseMode(mode))
    private fun cursor() = MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME))
}
