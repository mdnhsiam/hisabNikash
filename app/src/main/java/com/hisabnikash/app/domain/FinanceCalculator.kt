package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import java.time.ZoneId

object FinanceCalculator {
    fun dashboard(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        allocations: List<GoalAllocation>,
        month: MonthKey,
        zone: ZoneId = MonthKey.DHAKA,
    ): DashboardSummary {
        val current = transactions.inMonth(month, zone)
        var income = 0L
        var expense = 0L
        val byCategory = linkedMapOf<String?, Long>()
        current.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> income += transaction.amountMinor
                TransactionType.EXPENSE -> {
                    expense += transaction.amountMinor
                    byCategory[transaction.categoryId] = (byCategory[transaction.categoryId] ?: 0L) + transaction.amountMinor
                }
            }
        }
        val totalBudget = budgets.firstOrNull { it.monthKey == month.value && it.categoryId == null }
        val budgetStatus = totalBudget?.let {
            val remaining = it.amountMinor - expense
            BudgetStatus(
                limitMinor = it.amountMinor,
                spentMinor = expense,
                remainingMinor = remaining,
                percentUsed = if (it.amountMinor <= 0) 0 else ((expense * 100L) / it.amountMinor).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                isExceeded = remaining < 0,
            )
        }
        val allocationStart = month.startEpochMillis(zone)
        val allocationEnd = month.endExclusiveEpochMillis(zone)
        val allocated = allocations.asSequence()
            .filter { it.allocatedAtEpochMillis in allocationStart until allocationEnd }
            .sumOf(GoalAllocation::amountMinor)

        return DashboardSummary(
            month = month,
            incomeMinor = income,
            expenseMinor = expense,
            netSavingsMinor = income - expense,
            allocatedToGoalsMinor = allocated,
            budget = budgetStatus,
            recentTransactions = current.sortedWith(compareByDescending<Transaction> { it.occurredAtEpochMillis }.thenByDescending { it.createdAtEpochMillis }).take(5),
            expenseByCategory = byCategory.toSortedMap(compareBy(nullsLast()) { it }),
        )
    }

    fun monthlyReport(
        transactions: List<Transaction>,
        month: MonthKey,
        zone: ZoneId = MonthKey.DHAKA,
    ): MonthlyReport {
        val currentStart = month.startEpochMillis(zone)
        val currentEnd = month.endExclusiveEpochMillis(zone)
        val previousStart = month.previous().startEpochMillis(zone)
        var income = 0L
        var expense = 0L
        var previousIncome = 0L
        var previousExpense = 0L
        val byCategory = linkedMapOf<String?, Long>()
        val byPayment = linkedMapOf<PaymentMethod, Long>()

        transactions.forEach { transaction ->
            when (transaction.occurredAtEpochMillis) {
                in currentStart until currentEnd -> when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amountMinor
                    TransactionType.EXPENSE -> {
                        expense += transaction.amountMinor
                        byCategory[transaction.categoryId] = (byCategory[transaction.categoryId] ?: 0L) + transaction.amountMinor
                        byPayment[transaction.paymentMethod] = (byPayment[transaction.paymentMethod] ?: 0L) + transaction.amountMinor
                    }
                }
                in previousStart until currentStart -> when (transaction.type) {
                    TransactionType.INCOME -> previousIncome += transaction.amountMinor
                    TransactionType.EXPENSE -> previousExpense += transaction.amountMinor
                }
            }
        }
        val net = income - expense
        val previousNet = previousIncome - previousExpense
        return MonthlyReport(
            month = month,
            incomeMinor = income,
            expenseMinor = expense,
            netSavingsMinor = net,
            previousNetSavingsMinor = previousNet,
            netChangeMinor = net - previousNet,
            expenseByCategory = byCategory.toSortedMap(compareBy(nullsLast()) { it }),
            expenseByPaymentMethod = byPayment.toSortedMap(),
        )
    }
}

private fun List<Transaction>.inMonth(month: MonthKey, zone: ZoneId): List<Transaction> {
    val start = month.startEpochMillis(zone)
    val end = month.endExclusiveEpochMillis(zone)
    return filter { it.occurredAtEpochMillis in start until end }
}
