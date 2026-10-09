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
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
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

    @Test
    fun `manual and queued backup writes are serialized`() = runTest {
        val documents = FakeDocuments(writeDelayMillis = 30)
        val preferences = FakePreferences(UserPreferenceState(localBackupEnabled = true, localBackupUri = "content://provider/tree/backups"))
        val store = store(documents, preferences, FakeSecrets())

        listOf(
            async(Dispatchers.Default) { store.backUpNow("backup-pass".toCharArray()) },
            async(Dispatchers.Default) { store.backUpNow("backup-pass".toCharArray()) },
        ).forEach { it.await() }

        assertEquals(1, documents.maxConcurrentWrites.get())
    }

    @Test
    fun `disable waits for active write and cannot resurrect password or timestamp`() = runTest {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val documents = object : LocalDocumentAccess {
            override fun persistTreeAccess(uri: Uri) = true
            override fun read(uri: Uri) = byteArrayOf()
            override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) {
                entered.countDown()
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
            }
        }
        val preferences = FakePreferences(UserPreferenceState(localBackupEnabled = true, localBackupUri = "content://provider/tree/backups"))
        val secrets = FakeSecrets()
        val store = LocalBackupStore(documents, preferences, SnapshotExporter(database), SnapshotImporter(database, BackupCrypto(10_000)), secrets)
        val backup = async(Dispatchers.IO) { store.backUpNow("password9".toCharArray()) }
        check(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
        val disable = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { store.disable() }
        val disabledDuringWrite = disable.isCompleted
        release.countDown()
        backup.await()
        disable.await()

        assertFalse("disable must wait for the active write", disabledDuringWrite)
        assertFalse(preferences.state.value.localBackupEnabled)
        assertEquals(null, preferences.state.value.lastLocalBackupAtEpochMillis)
        assertEquals(null, secrets.password)
    }

    @Test
    fun `recovery snapshot capture occurs within replacement transaction`() = runTest {
        var capturedWithinTransaction = false
        val importer = SnapshotImporter(database, BackupCrypto(10_000))
        val documents = FakeDocuments().apply { readBytes = importer.encode(SnapshotExporter(database).export(), "password9".toCharArray()) }
        val recovery = object : com.hisabnikash.app.backup.RestoreRecoveryStore {
            override suspend fun save(password: CharArray) { capturedWithinTransaction = database.inTransaction() }
            override suspend fun restore(password: CharArray) = RestoreResult.Success
            override fun exists() = true
            override fun clear() = Unit
        }
        val store = LocalBackupStore(documents, FakePreferences(), SnapshotExporter(database), importer, FakeSecrets(), recovery)

        assertEquals(RestoreResult.Success, store.restoreFromDocument(Uri.parse("content://fixture"), "password9".toCharArray()))
        assertTrue("ledger writes must not interleave between recovery capture and replacement", capturedWithinTransaction)
    }

    @Test fun `successful disable clears secret even when timestamp cleanup fails`() = runTest {
        val preferences = object : LocalBackupPreferences {
            override val state = MutableStateFlow(UserPreferenceState(localBackupEnabled = true))
            override suspend fun setLocalBackup(enabled: Boolean, uri: String?) { state.value = state.value.copy(localBackupEnabled = enabled) }
            override suspend fun setLastLocalBackupAt(epochMillis: Long?) { error("fixture preference failure") }
        }
        val secrets = FakeSecrets().apply { save("password9".toCharArray()) }
        val store = LocalBackupStore(FakeDocuments(), preferences, SnapshotExporter(database), SnapshotImporter(database), secrets)
        assertTrue(runCatching { store.disable() }.isFailure)
        assertFalse(preferences.state.value.localBackupEnabled)
        assertEquals(null, secrets.password)
    }

    @Test fun `queued backup reads neither secret nor data after an earlier disable wins the gate`() = runTest {
        val gate = com.hisabnikash.app.backup.BackupOperationGate()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val holder = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { gate.run { release.await() } }
        var loads = 0
        var writes = 0
        val secrets = object : BackupPasswordCache {
            override fun load(): CharArray { loads++; return "password9".toCharArray() }
            override fun save(password: CharArray) = Unit
            override fun clear() = Unit
        }
        val documents = object : LocalDocumentAccess {
            override fun persistTreeAccess(uri: Uri) = true
            override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) { writes++ }
            override fun read(uri: Uri) = byteArrayOf()
        }
        val preferences = FakePreferences(UserPreferenceState(localBackupEnabled = true, localBackupUri = "content://fixture"))
        val store = LocalBackupStore(documents, preferences, SnapshotExporter(database), SnapshotImporter(database), secrets, operationGate = gate)
        val disable = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { store.disable() }
        val queued = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { store.backUpQueued() }
        assertEquals(0, loads)
        release.complete(Unit)
        holder.await(); disable.await(); queued.await()
        assertEquals(0, loads)
        assertEquals(0, writes)
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

private class FakeDocuments(private val available: Boolean = true, private val writeDelayMillis: Long = 0) : LocalDocumentAccess {
    var persistedTree: Uri? = null
    var lastFileName: String? = null
    var lastBytes: ByteArray? = null
    var readBytes: ByteArray = byteArrayOf()
    private val activeWrites = java.util.concurrent.atomic.AtomicInteger()
    val maxConcurrentWrites = java.util.concurrent.atomic.AtomicInteger()
    override fun persistTreeAccess(uri: Uri): Boolean { persistedTree = uri; return available }
    override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) {
        if (!available) throw BackupLocationUnavailableException()
        val active = activeWrites.incrementAndGet()
        maxConcurrentWrites.updateAndGet { maxOf(it, active) }
        try {
            if (writeDelayMillis > 0) Thread.sleep(writeDelayMillis)
            lastFileName = fileName
            lastBytes = bytes.copyOf()
        } finally { activeWrites.decrementAndGet() }
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
