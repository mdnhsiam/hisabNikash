package com.hisabnikash.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.data.db.HisabDatabase
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreRestoreBackupStoreTest {
    private lateinit var context: Context
    private lateinit var database: HisabDatabase
    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = HisabDatabase.createInMemory(context)
        listOf("pre-restore.hisab", "pre-restore.hisab.previous", "pre-restore.hisab.tmp").forEach { File(context.filesDir, it).delete() }
    }
    @After fun tearDown() { database.close() }

    @Test fun `interrupted replacement retains discoverable recovery snapshot`() = runTest {
        val importer = SnapshotImporter(database, BackupCrypto(10_000))
        val exporter = SnapshotExporter(database)
        val password = "password9".toCharArray()
        File(context.filesDir, "pre-restore.hisab.previous").writeBytes(importer.encode(exporter.export(), password))
        val store = PreRestoreBackupStore(context, exporter, importer)

        assertTrue("a parked recovery snapshot must survive process interruption", store.exists())
        assertEquals(RestoreResult.Success, store.restore(password))
    }
}
