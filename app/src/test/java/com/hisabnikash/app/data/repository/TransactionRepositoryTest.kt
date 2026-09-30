package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRepositoryTest {
    private val original = Transaction(
        id = "tx-1",
        type = TransactionType.EXPENSE,
        amountMinor = 12_500,
        description = "  বিদ্যুৎ   বিল ",
        categoryId = "bills",
        paymentMethod = PaymentMethod.BKASH,
        occurredAtEpochMillis = 1_000_000,
        createdAtEpochMillis = 1_000_000,
    )

    @Test
    fun `equivalent transaction within sixty seconds is a possible duplicate`() {
        val candidate = original.copy(
            id = "tx-2",
            description = "বিদ্যুৎ বিল",
            occurredAtEpochMillis = 9_999_999,
            createdAtEpochMillis = 1_059_999,
        )

        assertTrue(TransactionDuplicatePolicy.isPossibleDuplicate(original, candidate))
    }

    @Test
    fun `different fields or a later save are not duplicates`() {
        assertFalse(TransactionDuplicatePolicy.isPossibleDuplicate(original, original.copy(id = "later", createdAtEpochMillis = 1_060_001)))
        assertFalse(TransactionDuplicatePolicy.isPossibleDuplicate(original, original.copy(id = "amount", amountMinor = 12_501)))
        assertFalse(TransactionDuplicatePolicy.isPossibleDuplicate(original, original.copy(id = "method", paymentMethod = PaymentMethod.CASH)))
        assertFalse(TransactionDuplicatePolicy.isPossibleDuplicate(original, original.copy(id = "category", categoryId = "utilities")))
        assertFalse(TransactionDuplicatePolicy.isPossibleDuplicate(original, original.copy(id = "type", type = TransactionType.INCOME)))
    }
}
