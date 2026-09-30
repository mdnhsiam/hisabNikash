package com.hisabnikash.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.domain.BudgetStatus

@Composable
fun BudgetProgress(status: BudgetStatus, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (status.isExceeded) "Over budget by ${MoneyFormatter.formatMinorUnits(-status.remainingMinor)}"
            else "${MoneyFormatter.formatMinorUnits(status.remainingMinor)} remaining",
            color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        LinearProgressIndicator(
            progress = { (status.percentUsed / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text("${status.percentUsed}% used", style = MaterialTheme.typography.bodySmall)
    }
}
