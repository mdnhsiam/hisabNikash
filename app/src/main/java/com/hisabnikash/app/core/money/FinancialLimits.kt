package com.hisabnikash.app.core.money

/** Leaves room for signed comparisons, reversals and budget arithmetic without wrapping. */
object FinancialLimits {
    const val MAX_LEDGER_TOTAL_MINOR: Long = Long.MAX_VALUE / 4
    fun requirePositive(amount: Long) {
        require(amount in 1..MoneyParser.MAX_MINOR_UNITS) { "Amount is outside the supported range" }
    }
    fun total(amounts: Iterable<Long>): Long = amounts.fold(0L) { sum, amount -> Math.addExact(sum, amount) }
}
