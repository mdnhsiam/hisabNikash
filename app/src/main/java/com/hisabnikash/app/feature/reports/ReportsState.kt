package com.hisabnikash.app.feature.reports

import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.domain.MonthlyReport

data class ReportsState(
    val selectedMonth: MonthKey,
    val report: MonthlyReport,
)

internal fun emptyMonthlyReport(month: MonthKey) = MonthlyReport(
    month = month,
    incomeMinor = 0,
    expenseMinor = 0,
    netSavingsMinor = 0,
    previousNetSavingsMinor = 0,
    netChangeMinor = 0,
    expenseByCategory = emptyMap(),
    expenseByPaymentMethod = emptyMap(),
)
