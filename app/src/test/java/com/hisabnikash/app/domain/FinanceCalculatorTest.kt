package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorTest {
    private val dhaka = ZoneId.of("Asia/Dhaka")
    private val january = MonthKey(2025, 1)

    @Test
    fun `dashboard totals income expenses savings and budget`() {
        val transactions = listOf(
            transaction("salary", TransactionType.INCOME, 100_000, "income_salary", PaymentMethod.BANK, "2025-01-01T00:00:00Z"),
            transaction("food", TransactionType.EXPENSE, 30_000, "expense_food", PaymentMethod.CASH, "2025-01-02T00:00:00Z"),
            transaction("bus", TransactionType.EXPENSE, 5_000, "expense_transport", PaymentMethod.BKASH, "2025-01-03T00:00:00Z"),
        )
        val budgets = listOf(Budget("total", "2025-01", null, 50_000, 0))

        val summary = FinanceCalculator.dashboard(transactions, budgets, emptyList(), january)

        assertEquals(100_000, summary.incomeMinor)
        assertEquals(35_000, summary.expenseMinor)
        assertEquals(65_000, summary.netSavingsMinor)
        assertEquals(15_000L, summary.budget?.remainingMinor)
        assertEquals(70, summary.budget?.percentUsed)
        assertFalse(summary.budget?.isExceeded ?: true)
        assertEquals(listOf("bus", "food", "salary"), summary.recentTransactions.map { it.id })
    }

    @Test
    fun `negative savings is preserved as a deficit and exceeded budget is explicit`() {
        val summary = FinanceCalculator.dashboard(
            transactions = listOf(
                transaction("income", TransactionType.INCOME, 10_000, null, PaymentMethod.CASH, "2025-01-01T00:00:00Z"),
                transaction("expense", TransactionType.EXPENSE, 25_000, null, PaymentMethod.CASH, "2025-01-02T00:00:00Z"),
            ),
            budgets = listOf(Budget("total", "2025-01", null, 20_000, 0)),
            allocations = emptyList(),
            month = january,
        )

        assertEquals(-15_000, summary.netSavingsMinor)
        assertEquals(-5_000L, summary.budget?.remainingMinor)
        assertTrue(summary.budget?.isExceeded == true)
    }

    @Test
    fun `empty month has zero totals and no inferred budget`() {
        val summary = FinanceCalculator.dashboard(emptyList(), emptyList(), emptyList(), january)

        assertEquals(0, summary.incomeMinor)
        assertEquals(0, summary.expenseMinor)
        assertEquals(0, summary.netSavingsMinor)
        assertNull(summary.budget)
        assertTrue(summary.recentTransactions.isEmpty())
    }

    @Test
    fun `monthly report groups categories and payment methods and compares previous month`() {
        val transactions = listOf(
            transaction("dec", TransactionType.EXPENSE, 20_000, "expense_food", PaymentMethod.CASH, "2024-12-05T00:00:00Z"),
            transaction("jan-income", TransactionType.INCOME, 80_000, "income_salary", PaymentMethod.BANK, "2025-01-05T00:00:00Z"),
            transaction("jan-food", TransactionType.EXPENSE, 25_000, "expense_food", PaymentMethod.CASH, "2025-01-06T00:00:00Z"),
            transaction("jan-bus", TransactionType.EXPENSE, 5_000, "expense_transport", PaymentMethod.BKASH, "2025-01-07T00:00:00Z"),
        )

        val report = FinanceCalculator.monthlyReport(transactions, january)

        assertEquals(50_000, report.netSavingsMinor)
        assertEquals(-20_000, report.previousNetSavingsMinor)
        assertEquals(70_000, report.netChangeMinor)
        assertEquals(25_000L, report.expenseByCategory["expense_food"])
        assertEquals(5_000L, report.expenseByCategory["expense_transport"])
        assertEquals(25_000L, report.expenseByPaymentMethod[PaymentMethod.CASH])
        assertEquals(5_000L, report.expenseByPaymentMethod[PaymentMethod.BKASH])
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amount: Long,
        categoryId: String?,
        paymentMethod: PaymentMethod,
        instant: String,
    ) = Transaction(
        id = id,
        type = type,
        amountMinor = amount,
        description = id,
        categoryId = categoryId,
        paymentMethod = paymentMethod,
        occurredAtEpochMillis = Instant.parse(instant).toEpochMilli(),
        createdAtEpochMillis = Instant.parse(instant).toEpochMilli(),
    )
}
