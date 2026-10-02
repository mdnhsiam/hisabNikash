package com.hisabnikash.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.model.BackupEnvelope
import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.backup.model.BackupWireCodec
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
class RestoreValidatorTest {
    private lateinit var database: HisabDatabase
    private val crypto = BackupCrypto(10_000)

    @Before fun setUp() {
        database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After fun tearDown() = database.close()

    @Test(expected = UnsupportedBackupVersionException::class)
    fun `unsupported envelope version is rejected`() = runTest {
        val importer = SnapshotImporter(database, crypto)
        val unsupported = BackupEnvelope(formatVersion = 99, 10_000, "a", "b", "c")
        importer.validate(BackupWireCodec.encodeEnvelope(unsupported), "pass".toCharArray())
    }

    @Test(expected = InvalidBackupException::class)
    fun `snapshot missing a required table is rejected`() = runTest {
        val incompleteJson = """{"schemaVersion":1,"exportedAtEpochMillis":1,"transactions":[],"categories":[],"budgets":[],"savingsGoals":[],"recurringItems":[]}"""
        val envelope = crypto.encrypt(incompleteJson.encodeToByteArray(), "pass".toCharArray())
        SnapshotImporter(database, crypto).validate(BackupWireCodec.encodeEnvelope(envelope), "pass".toCharArray())
    }

    @Test
    fun `validation failure leaves current database unchanged`() = runTest {
        val repository = RoomTransactionRepository(database)
        repository.add(existingTransaction())
        val valid = BackupSnapshotV1.empty(exportedAtEpochMillis = 2)
        val envelope = crypto.encrypt(BackupWireCodec.encodeSnapshot(valid), "right".toCharArray())
        val bytes = BackupWireCodec.encodeEnvelope(envelope.copy(ciphertextBase64 = envelope.ciphertextBase64.reversed()))

        runCatching { SnapshotImporter(database, crypto).validate(bytes, "right".toCharArray()) }

        assertEquals(listOf(existingTransaction()), repository.observeAll().first())
    }

    private fun existingTransaction() = Transaction(
        id = "existing",
        type = TransactionType.INCOME,
        amountMinor = 10_000,
        description = "Salary",
        categoryId = "income_salary",
        paymentMethod = PaymentMethod.BANK,
        occurredAtEpochMillis = 1,
        createdAtEpochMillis = 1,
    )
}
