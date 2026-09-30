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
