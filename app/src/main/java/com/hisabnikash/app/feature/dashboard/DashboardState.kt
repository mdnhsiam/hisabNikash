package com.hisabnikash.app.feature.dashboard

import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.domain.DashboardSummary

data class DashboardState(
    val summary: DashboardSummary,
)

internal fun emptyDashboardSummary(month: MonthKey) = DashboardSummary(
    month = month,
    incomeMinor = 0,
    expenseMinor = 0,
    netSavingsMinor = 0,
    allocatedToGoalsMinor = 0,
    budget = null,
    recentTransactions = emptyList(),
    expenseByCategory = emptyMap(),
)
