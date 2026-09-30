package com.hisabnikash.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.money.MoneyFormatter

@Composable
fun TransactionRow(transaction: Transaction, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(transaction.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = (if (transaction.type == TransactionType.EXPENSE) "−" else "+") + MoneyFormatter.formatMinorUnits(transaction.amountMinor),
            color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
