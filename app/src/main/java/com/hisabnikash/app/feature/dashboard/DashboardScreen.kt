package com.hisabnikash.app.feature.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    // Deliberately not saveable: a new screen/activity must require explicit reveal.
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
    val toggleLabel = stringResource(if (revealed) R.string.hide_dashboard_details else R.string.show_dashboard_details)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.hisab_logo), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { revealed = !revealed }, modifier = Modifier.semantics { contentDescription = toggleLabel }) {
                val color = MaterialTheme.colorScheme.primary
                Canvas(Modifier.size(24.dp)) {
                    drawOval(color, topLeft = Offset(0f, size.height * .23f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * .54f), style = Stroke(2.dp.toPx()))
                    drawCircle(color, size.width * .12f, center)
                    if (!revealed) drawLine(color, Offset(0f, size.height), Offset(size.width, 0f), 2.dp.toPx())
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleMedium)
            Text(summary.month.localizedLabel(locale), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (revealed) {
                    Text(stringResource(R.string.monthly_remainder), style = MaterialTheme.typography.labelLarge)
                    Text(MoneyFormatter.formatMinorUnits(summary.netSavingsMinor, locale), style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .12f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f).clickable(role = Role.Button) { onViewType(TransactionType.INCOME) }.padding(vertical = 4.dp)) {
                            Text(stringResource(R.string.income), style = MaterialTheme.typography.labelMedium)
                            Text(MoneyFormatter.formatMinorUnits(summary.incomeMinor, locale), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(Modifier.weight(1f).clickable(role = Role.Button) { onViewType(TransactionType.EXPENSE) }.padding(vertical = 4.dp)) {
                            Text(stringResource(R.string.expense), style = MaterialTheme.typography.labelMedium)
                            Text(MoneyFormatter.formatMinorUnits(summary.expenseMinor, locale), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text(stringResource(R.string.monthly_remainder_help), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(stringResource(R.string.details_private), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.details_private_help), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (revealed) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_transactions), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onViewTransactions) { Text(stringResource(R.string.view_all)) }
            }
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
