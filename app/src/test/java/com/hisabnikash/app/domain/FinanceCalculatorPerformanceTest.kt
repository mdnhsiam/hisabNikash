package com.hisabnikash.app.domain

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import java.time.Instant
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorPerformanceTest {
    @Test
    fun `ten thousand transactions calculate within 250 milliseconds`() {
        val instant = Instant.parse("2025-01-15T00:00:00Z").toEpochMilli()
        val transactions = List(10_000) { index ->
            Transaction(
                id = index.toString(),
                type = if (index % 5 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                amountMinor = 100,
                description = "Item $index",
                categoryId = "category-${index % 10}",
                paymentMethod = PaymentMethod.entries[index % 4],
                occurredAtEpochMillis = instant,
                createdAtEpochMillis = instant,
            )
        }

        lateinit var report: MonthlyReport
        val elapsed = measureTimeMillis {
            report = FinanceCalculator.monthlyReport(transactions, MonthKey(2025, 1))
        }

        assertEquals(200_000, report.incomeMinor)
        assertEquals(800_000, report.expenseMinor)
        assertTrue("Calculation took ${elapsed}ms", elapsed < 250)
    }
}
