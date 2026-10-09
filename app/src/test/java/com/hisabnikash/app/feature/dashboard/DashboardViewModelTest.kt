package com.hisabnikash.app.feature.dashboard

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.feature.transactions.FakeTransactionRepository
import com.hisabnikash.app.feature.transactions.MainDispatcherRule
import com.hisabnikash.app.feature.transactions.transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `month rollover recalculates without another transaction emission`() = runTest {
        val repository = FakeTransactionRepository()
        val january = MonthKey(2025, 1)
        val february = january.next()
        repository.flow.value = listOf(transaction("jan").copy(amountMinor = 200, occurredAtEpochMillis = january.startEpochMillis()),
            transaction("feb").copy(amountMinor = 700, occurredAtEpochMillis = february.startEpochMillis()))
        val months = kotlinx.coroutines.flow.MutableStateFlow(january)
        val viewModel = DashboardViewModel(repository, currentMonth = { january }, months = months)
        advanceUntilIdle()
        assertEquals(200L, viewModel.state.value.summary.expenseMinor)
        months.value = february
        advanceUntilIdle()
        assertEquals(february, viewModel.state.value.summary.month)
        assertEquals(700L, viewModel.state.value.summary.expenseMinor)
    }

    @Test
    fun `repository changes recalculate current month immediately`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = DashboardViewModel(repository, currentMonth = { MonthKey(1970, 1) })
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.summary.expenseMinor)

        repository.flow.value = listOf(
            transaction("expense").copy(
                type = TransactionType.EXPENSE,
                amountMinor = 2_500,
                paymentMethod = PaymentMethod.CASH,
                occurredAtEpochMillis = 100_000,
            ),
        )
        advanceUntilIdle()

        assertEquals(2_500, viewModel.state.value.summary.expenseMinor)
        assertEquals(-2_500, viewModel.state.value.summary.netSavingsMinor)
    }
}
