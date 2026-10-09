package com.hisabnikash.app.feature.transactions

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.Transaction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.localizedLabel
import com.hisabnikash.app.core.time.MonthKey

@Composable
fun DuplicateWarningDialog(existing: Transaction, onSaveAnyway: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.possible_duplicate)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.possible_duplicate_message))
            Text(existing.description, style = MaterialTheme.typography.titleMedium)
            Text(MoneyFormatter.formatMinorUnits(existing.amountMinor))
            Text(existing.paymentMethod.localizedLabel())
            Text(java.time.Instant.ofEpochMilli(existing.occurredAtEpochMillis).atZone(MonthKey.DHAKA).toLocalDate().toString())
        } },
        confirmButton = { Button(onClick = onSaveAnyway) { Text(stringResource(R.string.save_anyway)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
}
