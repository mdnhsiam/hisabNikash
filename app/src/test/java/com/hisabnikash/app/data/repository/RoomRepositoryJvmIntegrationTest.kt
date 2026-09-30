package com.hisabnikash.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.db.HisabDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomRepositoryJvmIntegrationTest {
    private lateinit var database: HisabDatabase
    private lateinit var categories: RoomCategoryRepository
    private lateinit var transactions: RoomTransactionRepository

    @Before
    fun setUp() {
        database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        categories = RoomCategoryRepository(database.categoryDao())
        transactions = RoomTransactionRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `seeds categories and preserves archived category historical join`() = runTest {
        assertEquals(9, categories.observeActive(CategoryType.EXPENSE).first().size)
        val custom = Category("expense_pet", CategoryType.EXPENSE, "Pet", "পোষা প্রাণী", 50, false, true, 100)
        transactions.addWithCustomCategory(expense("tx-pet", custom.id), custom)

        categories.archive(custom.id)

        assertFalse(categories.observeActive(CategoryType.EXPENSE).first().any { it.id == custom.id })
        val historical = database.transactionDao().observeAllWithCategory().first().single()
        assertEquals("Pet", historical.category?.nameEn)
        assertFalse(historical.category?.isActive ?: true)
    }

    @Test
    fun `create update duplicate and delete persist through Room`() = runTest {
        val original = expense("tx-1", "expense_bills")
        transactions.add(original)
        assertNotNull(transactions.findPossibleDuplicate(original.copy(id = "tx-2", createdAtEpochMillis = 159_999)))

        transactions.update(original.copy(description = "Internet bill", updatedAtEpochMillis = 200_000))
        assertEquals("Internet bill", transactions.observeAll().first().single().description)

        transactions.delete(original.id)
        assertTrue(transactions.observeAll().first().isEmpty())
    }

    private fun expense(id: String, categoryId: String) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amountMinor = 12_500,
        description = "বিদ্যুৎ বিল",
        categoryId = categoryId,
        paymentMethod = PaymentMethod.BKASH,
        occurredAtEpochMillis = 100_000,
        createdAtEpochMillis = 100_000,
    )
}
