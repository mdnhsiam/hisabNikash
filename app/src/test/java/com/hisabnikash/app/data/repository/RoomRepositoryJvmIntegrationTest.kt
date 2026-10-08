package com.hisabnikash.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.SavingsGoal
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.data.db.HisabDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
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

    @Test
    fun `goal allocation and reversal are atomic auditable records`() = runTest {
        val savings = RoomSavingsRepository(database)
        savings.upsertGoal(SavingsGoal("goal", "Emergency", 100_000, createdAtEpochMillis = 10))
        transactions.add(Transaction("income-for-goal", TransactionType.INCOME, 80_000, "Income", null, paymentMethod = PaymentMethod.BANK, occurredAtEpochMillis = 1, createdAtEpochMillis = 1))
        savings.allocate(GoalAllocation("a1", "goal", 50_000, allocatedAtEpochMillis = 20))
        savings.reverse("a1", "r1", 30)

        val records = savings.observeAllocations().first()
        assertEquals(2, records.size)
        assertEquals(0L, records.sumOf { it.amountMinor })
        assertEquals("a1", records.last().reversesAllocationId)

        var rejected = false
        try {
            savings.allocate(GoalAllocation("too-much", "goal", 90_000, allocatedAtEpochMillis = 40))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun `concurrent allocations cannot exceed persisted lifetime savings`() = runTest {
        val savings = RoomSavingsRepository(database)
        transactions.add(Transaction("income", TransactionType.INCOME, 100_000, "Income", null, paymentMethod = PaymentMethod.BANK, occurredAtEpochMillis = 1, createdAtEpochMillis = 1))
        savings.upsertGoal(SavingsGoal("goal", "Emergency", null, createdAtEpochMillis = 2))

        val outcomes = listOf(
            async { runCatching { savings.allocate(GoalAllocation("a1", "goal", 60_000, allocatedAtEpochMillis = 3)) } },
            async { runCatching { savings.allocate(GoalAllocation("a2", "goal", 60_000, allocatedAtEpochMillis = 4)) } },
        ).map { it.await() }

        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(60_000L, savings.observeAllocations().first().sumOf { it.amountMinor })
    }

    @Test
    fun `recurring confirmation writes once while skip and disable never write`() = runTest {
        val recurringRepository = RoomRecurringRepository(database, transactions, nowEpochMillis = { 1_000 }, newId = { "confirmed" })
        val item = RecurringItem(
            id = "bill",
            type = TransactionType.EXPENSE,
            amountMinor = 5_000,
            description = "Electricity bill",
            categoryId = "expense_bills",
            paymentMethod = PaymentMethod.BKASH,
            nextDueAtEpochMillis = 100,
            createdAtEpochMillis = 0,
        )
        recurringRepository.upsert(item)

        recurringRepository.confirmOccurrence(item.id, expectedDueAtEpochMillis = item.nextDueAtEpochMillis)
        assertEquals(1, transactions.observeAll().first().size)
        val advanced = recurringRepository.find(item.id)!!
        assertTrue(advanced.nextDueAtEpochMillis > 1_000)

        val stale = runCatching { recurringRepository.confirmOccurrence(item.id, expectedDueAtEpochMillis = item.nextDueAtEpochMillis) }
        assertTrue(stale.isFailure)
        assertEquals(1, transactions.observeAll().first().size)

        recurringRepository.skipOccurrence(item.id)
        recurringRepository.disable(item.id)
        assertEquals(1, transactions.observeAll().first().size)
        assertFalse(recurringRepository.find(item.id)!!.isEnabled)
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
