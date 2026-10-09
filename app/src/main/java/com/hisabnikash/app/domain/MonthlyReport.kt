package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.time.MonthKey

data class MonthlyReport(
    val month: MonthKey,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netSavingsMinor: Long,
    val previousNetSavingsMinor: Long,
    val netChangeMinor: Long,
    val expenseByCategory: Map<String?, Long>,
    val expenseByPaymentMethod: Map<PaymentMethod, Long>,
    val dailyTotals: Map<java.time.LocalDate, DailyTotals> = emptyMap(),
)

data class DailyTotals(val incomeMinor: Long = 0, val expenseMinor: Long = 0)
