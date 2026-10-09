package com.hisabnikash.app.core.time

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Refresh on resume and while visible; consumers switch months even without a ledger write. */
class CurrentMonthClock(private val now: () -> Instant = Instant::now) {
    private val mutableMonth = MutableStateFlow(MonthKey.fromInstant(now()))
    val month: StateFlow<MonthKey> = mutableMonth
    fun refresh() { mutableMonth.value = MonthKey.fromInstant(now()) }
}
