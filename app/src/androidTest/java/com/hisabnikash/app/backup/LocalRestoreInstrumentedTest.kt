package com.hisabnikash.app.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.repository.RoomTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalRestoreInstrumentedTest {
    @Test
    fun clean_database_restores_encrypted_snapshot() = runBlocking {
        val source = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        val destination = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        try {
            val transaction = Transaction(
                id = "reinstall-restore",
                type = TransactionType.EXPENSE,
                amountMinor = 4_500,
                description = "বাজার",
                categoryId = "expense_groceries",
                paymentMethod = PaymentMethod.CASH,
                occurredAtEpochMillis = 10,
                createdAtEpochMillis = 10,
            )
            RoomTransactionRepository(source).add(transaction)
            val crypto = BackupCrypto(10_000)
            val sourceImporter = SnapshotImporter(source, crypto)
            val encrypted = sourceImporter.encode(SnapshotExporter(source).export(), "restore-pass".toCharArray())

            val destinationImporter = SnapshotImporter(destination, crypto)
            destinationImporter.apply(destinationImporter.validate(encrypted, "restore-pass".toCharArray()))

            assertEquals(listOf(transaction), RoomTransactionRepository(destination).observeAll().first())
        } finally {
            source.close()
            destination.close()
        }
    }
}
