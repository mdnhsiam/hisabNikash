package com.hisabnikash.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.repository.RoomTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapshotRoundTripTest {
    private lateinit var database: HisabDatabase

    @Before fun setUp() {
        database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After fun tearDown() = database.close()

    @Test
    fun `encrypted snapshot restores all transaction fields`() = runTest {
        val repository = RoomTransactionRepository(database)
        val original = Transaction(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 12_345,
            description = "বিদ্যুৎ বিল",
            categoryId = "expense_bills",
            paymentMethod = PaymentMethod.BKASH,
            note = "October",
            occurredAtEpochMillis = 1_700_000_000_000,
            createdAtEpochMillis = 1_700_000_000_100,
            updatedAtEpochMillis = 1_700_000_000_200,
        )
        repository.add(original)
        val exporter = SnapshotExporter(database)
        val importer = SnapshotImporter(database, BackupCrypto(10_000))
        val bytes = importer.encode(exporter.export(), "backup-pass".toCharArray())
        repository.clearAll()

        val validated = importer.validate(bytes, "backup-pass".toCharArray())
        assertEquals(RestoreResult.Success, importer.apply(validated))

        assertEquals(original, repository.observeAll().first().single())
    }
}
