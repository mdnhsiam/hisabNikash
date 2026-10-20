package com.hisabnikash.app.feature.transactions

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `dashboard drilldown filters type and Dhaka month while view all clears stale search`() = runTest {
        val month = MonthKey(2026, 10)
        val repository = FakeTransactionRepository()
        repository.flow.value = listOf(
            transaction("salary").copy(type = TransactionType.INCOME, occurredAtEpochMillis = month.startEpochMillis()),
            transaction("expense").copy(occurredAtEpochMillis = month.startEpochMillis()),
            transaction("old").copy(type = TransactionType.INCOME, occurredAtEpochMillis = month.startEpochMillis() - 1),
            transaction("next").copy(type = TransactionType.INCOME, occurredAtEpochMillis = month.endExclusiveEpochMillis()),
        )
        val model = TransactionListViewModel(repository)
        advanceUntilIdle()
        model.setQuery("stale search")
        model.setDashboardFilter(TransactionType.INCOME, month)
        assertEquals(listOf("salary"), model.state.value.visibleTransactions.map { it.id })
        model.setDashboardFilter(TransactionType.EXPENSE, month)
        assertEquals(listOf("expense"), model.state.value.visibleTransactions.map { it.id })
        model.setDashboardFilter(null, null)
        assertEquals(setOf("salary", "expense", "old", "next"), model.state.value.visibleTransactions.map { it.id }.toSet())
    }

    @Test
    fun `observes newest transactions and filters by query`() = runTest {
        val repository = FakeTransactionRepository()
        repository.flow.value = listOf(
            transaction("older").copy(description = "Bus", occurredAtEpochMillis = 10),
            transaction("newer").copy(description = "Lunch", occurredAtEpochMillis = 20),
        )
        val viewModel = TransactionListViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf("newer", "older"), viewModel.state.value.transactions.map { it.id })
        viewModel.setQuery("bus")
        assertEquals(listOf("older"), viewModel.state.value.visibleTransactions.map { it.id })
    }

    @Test
    fun `edit and confirmed delete update repository`() = runTest {
        val repository = FakeTransactionRepository()
        repository.flow.value = listOf(transaction("tx-1"))
        val viewModel = TransactionListViewModel(repository)
        advanceUntilIdle()

        viewModel.update(transaction("tx-1").copy(paymentMethod = PaymentMethod.BANK))
        advanceUntilIdle()
        assertEquals(PaymentMethod.BANK, repository.flow.value.single().paymentMethod)

        viewModel.delete("tx-1", confirmed = false)
        advanceUntilIdle()
        assertEquals(1, repository.flow.value.size)
        viewModel.delete("tx-1", confirmed = true)
        advanceUntilIdle()
        assertTrue(repository.flow.value.isEmpty())
    }
}
