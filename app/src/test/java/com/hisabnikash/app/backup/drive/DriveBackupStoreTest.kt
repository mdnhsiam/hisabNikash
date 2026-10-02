package com.hisabnikash.app.backup.drive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.data.db.HisabDatabase
import kotlinx.coroutines.test.runTest
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

    private class RecordingRemote : DriveRemoteStore {
        var uploaded: ByteArray? = null
        override suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray) { uploaded = encryptedEnvelope.copyOf() }
        override suspend fun downloadLatest(accessToken: String): ByteArray? = uploaded
    }
}
