package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.time.MonthKey

data class BudgetStatus(
    val limitMinor: Long,
    val spentMinor: Long,
    val remainingMinor: Long,
    val percentUsed: Int,
    val isExceeded: Boolean,
)

data class DashboardSummary(
    val month: MonthKey,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netSavingsMinor: Long,
    val allocatedToGoalsMinor: Long,
    val budget: BudgetStatus?,
    val recentTransactions: List<Transaction>,
    val expenseByCategory: Map<String?, Long>,
)
