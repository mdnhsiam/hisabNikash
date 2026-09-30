package com.hisabnikash.app.feature.transactions

import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.data.repository.PaymentMethodPreference
import com.hisabnikash.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickEntryViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `defaults to expense categories and last-used payment method`() = runTest {
        val fixture = Fixture(lastPaymentMethod = PaymentMethod.BKASH)
        advanceUntilIdle()

        assertEquals(TransactionType.EXPENSE, fixture.viewModel.state.value.type)
        assertEquals(PaymentMethod.BKASH, fixture.viewModel.state.value.paymentMethod)
        assertEquals(listOf("expense_food", "expense_other"), fixture.viewModel.state.value.categories.map { it.id })
        assertEquals("expense_food", fixture.viewModel.state.value.categoryId)
    }

    @Test
    fun `invalid required fields block submission and remain editable`() = runTest {
        val fixture = Fixture()
        fixture.viewModel.setAmount("0")
        fixture.viewModel.setDescription("  ")

        val result = fixture.viewModel.submit()

        assertTrue(result is SubmitResult.ValidationFailed)
        assertTrue(fixture.transactions.added.isEmpty())
        assertEquals("0", fixture.viewModel.state.value.amountInput)
        assertTrue(fixture.viewModel.state.value.errors.isNotEmpty())
    }

    @Test
    fun `Others can stay one-off or be saved as a reusable custom category`() = runTest {
        val oneOff = Fixture()
        oneOff.completeForm(categoryId = "expense_other", customLabel = "Pet", saveCategory = false)
        assertTrue(oneOff.viewModel.submit() is SubmitResult.Saved)
        assertEquals("Pet", oneOff.transactions.added.single().oneOffCategoryLabel)
        assertNull(oneOff.transactions.customCategory)

        val reusable = Fixture()
        reusable.completeForm(categoryId = "expense_other", customLabel = "Pet", saveCategory = true)
        assertTrue(reusable.viewModel.submit() is SubmitResult.Saved)
        assertEquals("Pet", reusable.transactions.customCategory?.nameEn)
        assertEquals(reusable.transactions.customCategory?.id, reusable.transactions.added.single().categoryId)
        assertNull(reusable.transactions.added.single().oneOffCategoryLabel)
    }

    @Test
    fun `duplicate warns first and intentional override saves`() = runTest {
        val fixture = Fixture()
        fixture.completeForm()
        fixture.transactions.possibleDuplicate = transaction(id = "existing")

        assertTrue(fixture.viewModel.submit() is SubmitResult.PossibleDuplicate)
        assertTrue(fixture.transactions.added.isEmpty())

        assertTrue(fixture.viewModel.submit(intentionalDuplicate = true) is SubmitResult.Saved)
        assertEquals(1, fixture.transactions.added.size)
    }

    @Test
    fun `retryable repository failure keeps form and successful save resets it`() = runTest {
        val fixture = Fixture()
        fixture.completeForm()
        fixture.transactions.failNextAdd = true

        assertTrue(fixture.viewModel.submit() is SubmitResult.Failed)
        assertEquals("125.50", fixture.viewModel.state.value.amountInput)
        assertEquals("Lunch", fixture.viewModel.state.value.description)

        assertTrue(fixture.viewModel.submit() is SubmitResult.Saved)
        assertEquals("", fixture.viewModel.state.value.amountInput)
        assertEquals("", fixture.viewModel.state.value.description)
        assertFalse(fixture.viewModel.state.value.isSaving)
        assertEquals(PaymentMethod.NAGAD, fixture.paymentPreference.value)
    }

    private class Fixture(lastPaymentMethod: PaymentMethod = PaymentMethod.CASH) {
        val transactions = FakeTransactionRepository()
        val categories = FakeCategoryRepository()
        val paymentPreference = FakePaymentMethodPreference(lastPaymentMethod)
        val viewModel = QuickEntryViewModel(
            transactionRepository = transactions,
            categoryRepository = categories,
            paymentMethodPreference = paymentPreference,
            nowEpochMillis = { 100_000 },
            newId = { "new-id" },
        )

        fun completeForm(
            categoryId: String = "expense_food",
            customLabel: String = "",
            saveCategory: Boolean = false,
        ) {
            viewModel.setAmount("125.50")
            viewModel.setDescription("Lunch")
            viewModel.setCategory(categoryId)
            viewModel.setCustomCategory(customLabel, saveCategory)
            viewModel.setPaymentMethod(PaymentMethod.NAGAD)
        }
    }
}

internal class FakeTransactionRepository : TransactionRepository {
    val flow = MutableStateFlow<List<Transaction>>(emptyList())
    val added = mutableListOf<Transaction>()
    var customCategory: Category? = null
    var possibleDuplicate: Transaction? = null
    var failNextAdd = false

    override fun observeAll(): Flow<List<Transaction>> = flow
    override suspend fun add(transaction: Transaction) = save(transaction)
    override suspend fun addWithCustomCategory(transaction: Transaction, category: Category) {
        customCategory = category
        save(transaction)
    }
    private fun save(transaction: Transaction) {
        if (failNextAdd) {
            failNextAdd = false
            error("disk full")
        }
        added += transaction
        flow.value = flow.value + transaction
    }
    override suspend fun update(transaction: Transaction) {
        flow.value = flow.value.map { if (it.id == transaction.id) transaction else it }
    }
    override suspend fun delete(id: String) {
        flow.value = flow.value.filterNot { it.id == id }
    }
    override suspend fun findPossibleDuplicate(candidate: Transaction): Transaction? = possibleDuplicate
}

internal class FakeCategoryRepository : CategoryRepository {
    private val expense = MutableStateFlow(
        listOf(
            Category("expense_food", CategoryType.EXPENSE, "Food", "খাবার", 0, true),
            Category("expense_other", CategoryType.EXPENSE, "Others", "অন্যান্য", 1, true),
        ),
    )
    private val income = MutableStateFlow(
        listOf(Category("income_salary", CategoryType.INCOME, "Salary", "বেতন", 0, true)),
    )
    override fun observeActive(type: CategoryType): Flow<List<Category>> = if (type == CategoryType.EXPENSE) expense else income
    override suspend fun add(category: Category) = Unit
    override suspend fun update(category: Category) = Unit
    override suspend fun archive(id: String) = Unit
}

internal class FakePaymentMethodPreference(initial: PaymentMethod) : PaymentMethodPreference {
    var value = initial
    override suspend fun get(): PaymentMethod = value
    override suspend fun set(method: PaymentMethod) { value = method }
}

internal fun transaction(id: String = "tx") = Transaction(
    id = id,
    type = TransactionType.EXPENSE,
    amountMinor = 12_550,
    description = "Lunch",
    categoryId = "expense_food",
    paymentMethod = PaymentMethod.NAGAD,
    occurredAtEpochMillis = 100_000,
    createdAtEpochMillis = 100_000,
)
