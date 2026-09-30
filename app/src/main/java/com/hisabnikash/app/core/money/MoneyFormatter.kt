package com.hisabnikash.app.core.money

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {
    fun formatMinorUnits(
        amountMinor: Long,
        locale: Locale = Locale.getDefault(),
        includeCurrency: Boolean = true,
    ): String {
        val amount = BigDecimal.valueOf(amountMinor, 2)
        val formatter = if (includeCurrency) NumberFormat.getCurrencyInstance(locale) else NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        if (includeCurrency) formatter.currency = Currency.getInstance("BDT")
        return formatter.format(amount)
    }
}
