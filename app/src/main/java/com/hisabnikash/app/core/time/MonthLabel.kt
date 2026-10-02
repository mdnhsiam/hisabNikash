package com.hisabnikash.app.core.time

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun MonthKey.localizedLabel(locale: Locale): String =
    YearMonth.parse(value).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
