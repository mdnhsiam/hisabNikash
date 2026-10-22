package com.hisabnikash.app.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.core.time.localizedLabel
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.ui.components.TransactionRow

@Composable
fun DashboardScreen(
    state: DashboardState,
    modifier: Modifier = Modifier,
    onViewTransactions: () -> Unit = {},
    onViewType: (TransactionType) -> Unit = {},
) {
    // Only recent entries are private. Monthly totals are always visible.
    var revealed by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) revealed = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val summary = state.summary
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.hisab_logo), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleMedium)
            Text(summary.month.localizedLabel(locale), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            Modifier.fillMaxWidth().heightIn(min = 240.dp), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.monthly_remainder), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(MoneyFormatter.formatMinorUnits(summary.netSavingsMinor, locale),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
                    color = if (summary.netSavingsMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer)
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .12f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { onViewType(TransactionType.INCOME) }.padding(vertical = 4.dp)) {
                        Text(stringResource(R.string.income), style = MaterialTheme.typography.labelLarge)
                        Text(MoneyFormatter.formatMinorUnits(summary.incomeMinor, locale),
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { onViewType(TransactionType.EXPENSE) }.padding(vertical = 4.dp)) {
                        Text(stringResource(R.string.expense), style = MaterialTheme.typography.labelLarge)
                        Text(MoneyFormatter.formatMinorUnits(summary.expenseMinor, locale),
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(stringResource(R.string.monthly_remainder_help), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_transactions), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { revealed = !revealed }) {
                Text(stringResource(if (revealed) R.string.hide_recent_transactions else R.string.show_recent_transactions))
            }
        }
        if (revealed) {
            TextButton(onClick = onViewTransactions) { Text(stringResource(R.string.view_all)) }
            if (summary.recentTransactions.isEmpty()) {
                Text(stringResource(R.string.no_transactions_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    summary.recentTransactions.take(3).forEachIndexed { index, transaction ->
                        TransactionRow(transaction)
                        if (index < minOf(2, summary.recentTransactions.lastIndex)) HorizontalDivider()
                    }
                }
            }
        } else {
            Text(stringResource(R.string.recent_transactions_hidden), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
