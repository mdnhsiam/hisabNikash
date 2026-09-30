package com.hisabnikash.app.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryInstrumentedTest {
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
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedsBothCategoryTypes() = runTest {
        assertEquals(9, categories.observeActive(CategoryType.EXPENSE).first().size)
        assertEquals(4, categories.observeActive(CategoryType.INCOME).first().size)
    }

    @Test
    fun customCategoryPersistsAndArchivedCategoryStillJoinsHistory() = runTest {
        val category = Category(
            id = "expense_pet",
            type = CategoryType.EXPENSE,
            nameEn = "Pet",
            nameBn = "পোষা প্রাণী",
            sortOrder = 50,
            isSystem = false,
            createdAtEpochMillis = 100,
        )
        categories.add(category)
        val transaction = expense(id = "tx-pet", categoryId = category.id)
        transactions.add(transaction)

        assertTrue(categories.observeActive(CategoryType.EXPENSE).first().any { it.id == category.id })
        categories.archive(category.id)

        assertFalse(categories.observeActive(CategoryType.EXPENSE).first().any { it.id == category.id })
        val historical = database.transactionDao().observeAllWithCategory().first().single()
        assertEquals("Pet", historical.category?.nameEn)
        assertFalse(historical.category?.isActive ?: true)
    }

    @Test
    fun transactionCreateUpdateDuplicateLookupAndDeleteRoundTrip() = runTest {
        val original = expense(id = "tx-1", categoryId = "expense_bills")
        transactions.add(original)
        assertEquals(listOf(original), transactions.observeAll().first())

        val candidate = original.copy(id = "tx-2", description = " বিদ্যুৎ   বিল ", createdAtEpochMillis = 159_999)
        assertNotNull(transactions.findPossibleDuplicate(candidate))

        transactions.update(original.copy(description = "Internet bill", updatedAtEpochMillis = 200_000))
        assertEquals("Internet bill", transactions.observeAll().first().single().description)

        transactions.delete(original.id)
        assertTrue(transactions.observeAll().first().isEmpty())
        assertNull(transactions.findPossibleDuplicate(candidate))
    }

    @Test
    fun addingCustomCategoryAndTransactionIsAtomic() = runTest {
        val category = Category("expense_hobby", CategoryType.EXPENSE, "Hobby", "শখ", 60, false, true, 100)
        transactions.addWithCustomCategory(expense("tx-hobby", category.id), category)

        assertEquals("expense_hobby", transactions.observeAll().first().single().categoryId)
        assertNotNull(database.categoryDao().findById(category.id))
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
