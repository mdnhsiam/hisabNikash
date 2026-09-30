package com.hisabnikash.app.core.time

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthKey(
    val year: Int,
    val month: Int,
) {
    init {
        require(month in 1..12) { "Month must be from 1 through 12" }
    }

    val value: String get() = "%04d-%02d".format(year, month)

    fun previous(): MonthKey = fromYearMonth(yearMonth.minusMonths(1))
    fun next(): MonthKey = fromYearMonth(yearMonth.plusMonths(1))

    fun startEpochMillis(zone: ZoneId = DHAKA): Long = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun endExclusiveEpochMillis(zone: ZoneId = DHAKA): Long = next().startEpochMillis(zone)

    private val yearMonth: YearMonth get() = YearMonth.of(year, month)

    companion object {
        val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")

        fun fromInstant(instant: Instant, zone: ZoneId = DHAKA): MonthKey {
            val date = instant.atZone(zone)
            return MonthKey(date.year, date.monthValue)
        }

        fun parse(value: String): MonthKey = fromYearMonth(YearMonth.parse(value))

        private fun fromYearMonth(value: YearMonth) = MonthKey(value.year, value.monthValue)
    }
}
