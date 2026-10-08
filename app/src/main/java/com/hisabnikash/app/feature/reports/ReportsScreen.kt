package com.hisabnikash.app.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.SummaryCard
import com.hisabnikash.app.ui.components.localizedLabel
import com.hisabnikash.app.core.time.localizedLabel
import com.hisabnikash.app.ui.components.localizedName

@Composable
fun ReportsScreen(
    state: ReportsState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = state.report
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPreviousMonth) { Text(stringResource(R.string.previous_month)) }
            Text(report.month.localizedLabel(locale), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNextMonth) { Text(stringResource(R.string.next_month)) }
        }
        SummaryCard(stringResource(R.string.monthly_savings), MoneyFormatter.formatMinorUnits(report.netSavingsMinor))
        SummaryCard(stringResource(R.string.previous_month_change), signedMoney(report.netChangeMinor))
        Text(stringResource(R.string.by_category), style = MaterialTheme.typography.titleMedium)
        if (report.expenseByCategory.isEmpty()) Text(stringResource(R.string.no_expenses_this_month))
        report.expenseByCategory.forEach { (category, amount) ->
            BreakdownRow(category?.let { state.categories[it]?.localizedName(locale.language == "bn") ?: it } ?: stringResource(R.string.uncategorized), amount)
        }
        Text(stringResource(R.string.by_payment_method), style = MaterialTheme.typography.titleMedium)
        report.expenseByPaymentMethod.forEach { (method, amount) -> BreakdownRow(method.localizedLabel(), amount) }
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(MoneyFormatter.formatMinorUnits(amount))
    }
}

private fun signedMoney(amount: Long): String = (if (amount >= 0) "+" else "") + MoneyFormatter.formatMinorUnits(amount)
