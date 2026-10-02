package com.hisabnikash.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.BudgetProgress
import com.hisabnikash.app.ui.components.SummaryCard
import com.hisabnikash.app.ui.components.TransactionRow
import com.hisabnikash.app.core.time.localizedLabel

@Composable
fun DashboardScreen(state: DashboardState, modifier: Modifier = Modifier) {
    val summary = state.summary
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineMedium)
        Text(summary.month.localizedLabel(locale), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SummaryCard(stringResource(R.string.monthly_balance), MoneyFormatter.formatMinorUnits(summary.netSavingsMinor))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(stringResource(R.string.income), MoneyFormatter.formatMinorUnits(summary.incomeMinor), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            SummaryCard(stringResource(R.string.expense), MoneyFormatter.formatMinorUnits(summary.expenseMinor), Modifier.weight(1f), MaterialTheme.colorScheme.error)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.monthly_budget), style = MaterialTheme.typography.titleMedium)
                summary.budget?.let { BudgetProgress(it) }
                    ?: Text(stringResource(R.string.budget_setup_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium)
        if (summary.recentTransactions.isEmpty()) {
            Text(stringResource(R.string.no_transactions_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            summary.recentTransactions.forEach { TransactionRow(it) }
        }
    }
}
