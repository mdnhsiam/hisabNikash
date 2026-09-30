package com.hisabnikash.app.feature.transactions

import com.hisabnikash.app.core.model.PaymentMethod
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
