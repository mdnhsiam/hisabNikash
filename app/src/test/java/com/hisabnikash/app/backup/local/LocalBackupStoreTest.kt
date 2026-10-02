package com.hisabnikash.app.backup.local

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.RestoreResult
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.preferences.LocalBackupPreferences
import com.hisabnikash.app.data.preferences.UserPreferenceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalBackupStoreTest {
    private lateinit var database: HisabDatabase

    @Before fun setUp() {
        database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After fun tearDown() = database.close()

    @Test
    fun `location permission persists and manual backup uses atomic replacement`() = runTest {
        val documents = FakeDocuments()
        val preferences = FakePreferences()
        val secrets = FakeSecrets()
        val store = store(documents, preferences, secrets)
        val tree = Uri.parse("content://provider/tree/backups")

        store.chooseBackupLocation(tree)
        store.backUpNow("backup-pass".toCharArray())

        assertEquals(tree, documents.persistedTree)
        assertTrue(preferences.state.value.localBackupEnabled)
        assertEquals(tree.toString(), preferences.state.value.localBackupUri)
        assertEquals(LocalBackupStore.FILE_NAME, documents.lastFileName)
        assertTrue(documents.lastBytes!!.isNotEmpty())
        assertArrayEquals("backup-pass".toCharArray(), secrets.password)
        assertEquals(500L, preferences.state.value.lastLocalBackupAtEpochMillis)
    }

    @Test(expected = BackupLocationUnavailableException::class)
    fun `unavailable removable location fails without disabling local data`() = runTest {
        val documents = FakeDocuments(available = false)
        val preferences = FakePreferences(UserPreferenceState(localBackupEnabled = true, localBackupUri = "content://missing"))
        store(documents, preferences, FakeSecrets()).backUpNow("passphrase".toCharArray())
    }

    @Test
    fun `corrupt restore preserves current database`() = runTest {
        val documents = FakeDocuments().apply { readBytes = "not-a-backup".encodeToByteArray() }
        val preferences = FakePreferences()
        val before = SnapshotExporter(database).export()

        val result = runCatching {
            store(documents, preferences, FakeSecrets()).restoreFromDocument(Uri.parse("content://bad"), "passphrase".toCharArray())
        }

        assertTrue(result.isFailure)
        assertEquals(before.copy(exportedAtEpochMillis = 0), SnapshotExporter(database) { 0 }.export())
    }

    private fun store(documents: FakeDocuments, preferences: FakePreferences, secrets: FakeSecrets) = LocalBackupStore(
        documentAccess = documents,
        preferences = preferences,
        exporter = SnapshotExporter(database) { 100 },
        importer = SnapshotImporter(database, BackupCrypto(10_000)),
        passwordCache = secrets,
        nowEpochMillis = { 500 },
    )
}

private class FakeDocuments(private val available: Boolean = true) : LocalDocumentAccess {
    var persistedTree: Uri? = null
    var lastFileName: String? = null
    var lastBytes: ByteArray? = null
    var readBytes: ByteArray = byteArrayOf()
    override fun persistTreeAccess(uri: Uri): Boolean { persistedTree = uri; return available }
    override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) {
        if (!available) throw BackupLocationUnavailableException()
        lastFileName = fileName
        lastBytes = bytes.copyOf()
    }
    override fun read(uri: Uri): ByteArray = readBytes
}

private class FakePreferences(initial: UserPreferenceState = UserPreferenceState()) : LocalBackupPreferences {
    override val state = MutableStateFlow(initial)
    override suspend fun setLocalBackup(enabled: Boolean, uri: String?) { state.value = state.value.copy(localBackupEnabled = enabled, localBackupUri = uri) }
    override suspend fun setLastLocalBackupAt(epochMillis: Long?) { state.value = state.value.copy(lastLocalBackupAtEpochMillis = epochMillis) }
}

private class FakeSecrets : BackupPasswordCache {
    var password: CharArray? = null
    override fun save(password: CharArray) { this.password = password.copyOf() }
    override fun load(): CharArray? = password?.copyOf()
    override fun clear() { password?.fill('\u0000'); password = null }
}
