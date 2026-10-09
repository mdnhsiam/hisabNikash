package com.hisabnikash.app.backup.drive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.data.db.HisabDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriveBackupStoreTest {
    private lateinit var database: HisabDatabase
    @Before fun setUp() { database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext<Context>()) }
    @After fun tearDown() = database.close()

    @Test
    fun `only authenticated ciphertext is sent to Drive`() = runTest {
        val remote = RecordingRemote()
        val store = DriveBackupStore(remote, SnapshotExporter(database), SnapshotImporter(database, BackupCrypto(10_000)))

        store.backUpNow("token", "password9".toCharArray())

        val body = remote.uploaded!!
        assertTrue(body.isNotEmpty())
        assertFalse(body.decodeToString().contains("transactions"))
        assertFalse(body.decodeToString().contains("categories"))
    }

    @Test
    fun `restore cannot download while an older upload is unfinished`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var downloads = 0
        val remote = object : DriveRemoteStore {
            override suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray) {
                entered.complete(Unit)
                release.await()
            }
            override suspend fun downloadLatest(accessToken: String): ByteArray? { downloads++; return null }
        }
        val store = DriveBackupStore(remote, SnapshotExporter(database), SnapshotImporter(database, BackupCrypto(10_000)))
        val upload = async { store.backUpNow("token", "password9".toCharArray()) }
        entered.await()
        val restore = async(start = CoroutineStart.UNDISPATCHED) { runCatching { store.restoreLatest("token", "password9".toCharArray()) } }
        val downloadsWhileUploadActive = downloads
        release.complete(Unit)
        upload.await()
        restore.await()

        assertEquals("all Drive operations must share the same operation gate", 0, downloadsWhileUploadActive)
        assertEquals(1, downloads)
    }

    @Test fun `snapshot export failure reports failed rather than leaving status running`() = runTest {
        val store = DriveBackupStore(RecordingRemote(), SnapshotExporter(database), SnapshotImporter(database, BackupCrypto(10_000)))
        database.close()
        assertTrue(runCatching { store.backUpNow("fixture-token", "password9".toCharArray()) }.isFailure)
        assertTrue(store.status.value is BackupStatus.Failed)
    }

    private class RecordingRemote : DriveRemoteStore {
        var uploaded: ByteArray? = null
        override suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray) { uploaded = encryptedEnvelope.copyOf() }
        override suspend fun downloadLatest(accessToken: String): ByteArray? = uploaded
    }
}
