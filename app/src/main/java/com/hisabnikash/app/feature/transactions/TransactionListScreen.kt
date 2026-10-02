package com.hisabnikash.app.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.localizedLabel

@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onQueryChange: (String) -> Unit,
    onUpdate: (Transaction) -> Unit,
    onDelete: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.search_transactions)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.visibleTransactions.isEmpty()) {
            EmptyState(stringResource(R.string.no_transactions), stringResource(R.string.no_transactions_message))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(state.visibleTransactions, key = Transaction::id) { transaction ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { editing = transaction }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(transaction.description, style = MaterialTheme.typography.titleMedium)
                                Text(transaction.paymentMethod.localizedLabel(), style = MaterialTheme.typography.bodySmall)
                            }
                            Column {
                                Text(
                                    text = (if (transaction.type == TransactionType.EXPENSE) "−" else "+") + MoneyFormatter.formatMinorUnits(transaction.amountMinor),
                                    color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                )
                                TextButton(onClick = { deleting = transaction }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { transaction ->
        var description by remember(transaction.id) { mutableStateOf(transaction.description) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_transaction)) },
            text = {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.description)) })
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(transaction.copy(description = description.trim(), updatedAtEpochMillis = System.currentTimeMillis()))
                    editing = null
                }, enabled = description.isNotBlank()) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = { Text(stringResource(R.string.delete_transaction_message)) },
            confirmButton = {
                Button(onClick = {
                    onDelete(transaction.id, true)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
