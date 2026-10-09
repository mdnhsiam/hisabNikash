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
                TransactionType.INCOME -> income = Math.addExact(income, transaction.amountMinor)
                TransactionType.EXPENSE -> {
                    expense = Math.addExact(expense, transaction.amountMinor)
                    byCategory[transaction.categoryId] = Math.addExact(byCategory[transaction.categoryId] ?: 0L, transaction.amountMinor)
                }
            }
        }
        val totalBudget = budgets.firstOrNull { it.monthKey == month.value && it.categoryId == null }
        val budgetStatus = totalBudget?.let {
            budgetStatus(it.amountMinor, expense)
        }
        val categoryBudgets = budgets.asSequence()
            .filter { it.monthKey == month.value && it.categoryId != null }
            .associate { budget ->
                budget.categoryId!! to budgetStatus(budget.amountMinor, byCategory[budget.categoryId] ?: 0L)
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
            unallocatedSavingsMinor = (income - expense) - allocated,
            budget = budgetStatus,
            categoryBudgets = categoryBudgets,
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
        val daily = linkedMapOf<java.time.LocalDate, DailyTotals>()

        transactions.forEach { transaction ->
            if (transaction.occurredAtEpochMillis in currentStart until currentEnd) {
                val date = java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(zone).toLocalDate()
                val previous = daily[date] ?: DailyTotals()
                daily[date] = if (transaction.type == TransactionType.INCOME)
                    previous.copy(incomeMinor = Math.addExact(previous.incomeMinor, transaction.amountMinor))
                else previous.copy(expenseMinor = Math.addExact(previous.expenseMinor, transaction.amountMinor))
            }
            when (transaction.occurredAtEpochMillis) {
                in currentStart until currentEnd -> when (transaction.type) {
                    TransactionType.INCOME -> income = Math.addExact(income, transaction.amountMinor)
                    TransactionType.EXPENSE -> {
                        expense = Math.addExact(expense, transaction.amountMinor)
                        byCategory[transaction.categoryId] = Math.addExact(byCategory[transaction.categoryId] ?: 0L, transaction.amountMinor)
                        byPayment[transaction.paymentMethod] = Math.addExact(byPayment[transaction.paymentMethod] ?: 0L, transaction.amountMinor)
                    }
                }
                in previousStart until currentStart -> when (transaction.type) {
                    TransactionType.INCOME -> previousIncome = Math.addExact(previousIncome, transaction.amountMinor)
                    TransactionType.EXPENSE -> previousExpense = Math.addExact(previousExpense, transaction.amountMinor)
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
            netChangeMinor = Math.subtractExact(net, previousNet),
            expenseByCategory = byCategory.toSortedMap(compareBy(nullsLast()) { it }),
            expenseByPaymentMethod = byPayment.toSortedMap(),
            dailyTotals = daily.toSortedMap(reverseOrder()),
        )
    }
}

private fun budgetStatus(limitMinor: Long, spentMinor: Long): BudgetStatus {
    val remaining = limitMinor - spentMinor
    return BudgetStatus(
        limitMinor = limitMinor,
        spentMinor = spentMinor,
        remainingMinor = remaining,
        percentUsed = if (limitMinor <= 0) 0 else java.math.BigInteger.valueOf(spentMinor).multiply(java.math.BigInteger.valueOf(100))
            .divide(java.math.BigInteger.valueOf(limitMinor)).min(java.math.BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt(),
        isExceeded = remaining < 0,
    )
}

private fun List<Transaction>.inMonth(month: MonthKey, zone: ZoneId): List<Transaction> {
    val start = month.startEpochMillis(zone)
    val end = month.endExclusiveEpochMillis(zone)
    return filter { it.occurredAtEpochMillis in start until end }
}
