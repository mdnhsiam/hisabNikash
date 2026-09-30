package com.hisabnikash.app.feature.budgets

import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.BudgetRepository
import com.hisabnikash.app.feature.transactions.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts unconfigured and saves total and category budgets`() = runTest {
        val repository = FakeBudgetRepository()
        val viewModel = BudgetViewModel(repository, MonthKey(2025, 1), nowEpochMillis = { 100 }, newId = { "id" })
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isConfigured)

        assertTrue(viewModel.saveBudget("5000", categoryId = null) is BudgetSaveResult.Saved)
        assertTrue(viewModel.saveBudget("1000", categoryId = "expense_food") is BudgetSaveResult.Saved)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isConfigured)
        assertEquals(500_000L, viewModel.state.value.totalBudget?.amountMinor)
        assertEquals(100_000L, viewModel.state.value.categoryBudgets.single().amountMinor)
    }

    @Test
    fun `invalid amount does not create a budget`() = runTest {
        val repository = FakeBudgetRepository()
        val viewModel = BudgetViewModel(repository, MonthKey(2025, 1))

        assertTrue(viewModel.saveBudget("0", null) is BudgetSaveResult.InvalidAmount)
        assertTrue(repository.flow.value.isEmpty())
    }
}

internal class FakeBudgetRepository : BudgetRepository {
    val flow = MutableStateFlow<List<Budget>>(emptyList())
    override fun observeMonth(month: MonthKey): Flow<List<Budget>> = flow
    override suspend fun upsert(budget: Budget) {
        flow.value = flow.value.filterNot { it.monthKey == budget.monthKey && it.categoryId == budget.categoryId } + budget
    }
    override suspend fun delete(id: String) { flow.value = flow.value.filterNot { it.id == id } }
}
