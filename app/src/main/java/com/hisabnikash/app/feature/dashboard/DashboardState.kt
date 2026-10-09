package com.hisabnikash.app.feature.dashboard

import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.domain.DashboardSummary

data class DashboardState(
    val summary: DashboardSummary,
    val categories: Map<String, com.hisabnikash.app.core.model.Category> = emptyMap(),
)

internal fun emptyDashboardSummary(month: MonthKey) = DashboardSummary(
    month = month,
    incomeMinor = 0,
    expenseMinor = 0,
    netSavingsMinor = 0,
    allocatedToGoalsMinor = 0,
    unallocatedSavingsMinor = 0,
    budget = null,
    categoryBudgets = emptyMap(),
    recentTransactions = emptyList(),
    expenseByCategory = emptyMap(),
)
