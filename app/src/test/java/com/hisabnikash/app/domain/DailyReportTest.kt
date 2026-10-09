package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.*
import com.hisabnikash.app.core.time.MonthKey
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyReportTest {
    private fun transaction(id: String, type: TransactionType, amount: Long, at: String) = Transaction(
        id, type, amount, "Synthetic fixture", null, paymentMethod = PaymentMethod.CASH,
        occurredAtEpochMillis = Instant.parse(at).toEpochMilli(), createdAtEpochMillis = 0)

    @Test fun `daily totals use Dhaka dates and only the selected month`() {
        val report = FinanceCalculator.monthlyReport(listOf(
            transaction("prior", TransactionType.EXPENSE, 999, "2025-01-31T17:59:59Z"),
            transaction("income", TransactionType.INCOME, 1000, "2025-01-31T18:00:00Z"),
            transaction("expense", TransactionType.EXPENSE, 200, "2025-02-01T17:59:59Z"),
            transaction("next-day", TransactionType.EXPENSE, 500, "2025-02-01T18:00:00Z")), MonthKey(2025, 2))
        assertEquals(listOf(LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 1)), report.dailyTotals.keys.toList())
        assertEquals(DailyTotals(1000, 200), report.dailyTotals[LocalDate.of(2025, 2, 1)])
        assertEquals(report.incomeMinor, report.dailyTotals.values.sumOf { it.incomeMinor })
        assertEquals(report.expenseMinor, report.dailyTotals.values.sumOf { it.expenseMinor })
    }

    @Test fun `several years of synthetic records retain exact daily and monthly totals`() {
        val records = (0 until 10_000).map { index ->
            transaction("t-$index", if (index % 2 == 0) TransactionType.INCOME else TransactionType.EXPENSE, 100,
                Instant.ofEpochMilli(MonthKey(2020, 1).startEpochMillis() + index * 86_400_000L).toString())
        }
        val month = MonthKey(2025, 1)
        val report = FinanceCalculator.monthlyReport(records, month)
        assertEquals(31, report.dailyTotals.size)
        assertEquals(3100L, report.incomeMinor + report.expenseMinor)
        assertEquals(report.netSavingsMinor, FinanceCalculator.dashboard(records, emptyList(), emptyList(), month).netSavingsMinor)
    }
}
