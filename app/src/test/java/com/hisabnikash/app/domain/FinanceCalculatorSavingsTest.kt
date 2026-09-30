package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceCalculatorSavingsTest {
    private val month = MonthKey(2025, 1)

    @Test
    fun `budgets remain optional and category budget states are independent`() {
        val expense = transaction("food", TransactionType.EXPENSE, 12_000, "expense_food")
        assertNull(FinanceCalculator.dashboard(listOf(expense), emptyList(), emptyList(), month).budget)

        val summary = FinanceCalculator.dashboard(
            listOf(expense),
            listOf(
                Budget("total", month.value, null, 20_000, 0),
                Budget("food", month.value, "expense_food", 10_000, 0),
            ),
            emptyList(),
            month,
        )

        assertEquals(-2_000L, summary.categoryBudgets.getValue("expense_food").remainingMinor)
        assertEquals(true, summary.categoryBudgets.getValue("expense_food").isExceeded)
        assertEquals(8_000L, summary.budget?.remainingMinor)
    }

    @Test
    fun `allocations and reversals change earmarked amount but never net savings`() {
        val transactions = listOf(
            transaction("income", TransactionType.INCOME, 100_000, "income_salary"),
            transaction("expense", TransactionType.EXPENSE, 20_000, "expense_food"),
        )
        val allocations = listOf(
            GoalAllocation("a1", "goal", 30_000, allocatedAtEpochMillis = instant),
            GoalAllocation("r1", "goal", -10_000, allocatedAtEpochMillis = instant, reversesAllocationId = "a1"),
        )

        val summary = FinanceCalculator.dashboard(transactions, emptyList(), allocations, month)

        assertEquals(80_000, summary.netSavingsMinor)
        assertEquals(20_000, summary.allocatedToGoalsMinor)
        assertEquals(60_000, summary.unallocatedSavingsMinor)
    }

    @Test
    fun `deficit remains negative available savings`() {
        val summary = FinanceCalculator.dashboard(
            listOf(
                transaction("income", TransactionType.INCOME, 10_000, null),
                transaction("expense", TransactionType.EXPENSE, 15_000, null),
            ),
            emptyList(),
            emptyList(),
            month,
        )

        assertEquals(-5_000, summary.netSavingsMinor)
        assertEquals(-5_000, summary.unallocatedSavingsMinor)
    }

    private val instant = Instant.parse("2025-01-15T00:00:00Z").toEpochMilli()
    private fun transaction(id: String, type: TransactionType, amount: Long, categoryId: String?) = Transaction(
        id = id,
        type = type,
        amountMinor = amount,
        description = id,
        categoryId = categoryId,
        paymentMethod = PaymentMethod.CASH,
        occurredAtEpochMillis = instant,
        createdAtEpochMillis = instant,
    )
}
