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
import com.hisabnikash.app.ui.components.localizedName
import androidx.compose.ui.platform.LocalConfiguration
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.ui.components.CategoryDropdown
import com.hisabnikash.app.ui.components.PaymentMethodDropdown
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hisabnikash.app.core.time.DateInput
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.core.time.localizedLabel

@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onQueryChange: (String) -> Unit,
    onUpdate: suspend (Transaction) -> Boolean,
    onDelete: suspend (String, Boolean) -> Boolean,
    modifier: Modifier = Modifier,
    onClearFilter: () -> Unit = {},
) {
    val useBangla = LocalConfiguration.current.locales[0].language == "bn"
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
        if (state.typeFilter != null || state.monthFilter != null) {
            Text(
                listOfNotNull(state.monthFilter?.localizedLabel(LocalConfiguration.current.locales[0]),
                    state.typeFilter?.let { stringResource(if (it == TransactionType.INCOME) R.string.income else R.string.expense) }).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(onClick = onClearFilter) { Text(stringResource(R.string.view_all)) }
        }
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
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(transaction.description, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(
                                    transaction.oneOffCategoryLabel
                                        ?: transaction.categoryId?.let { state.categories[it]?.localizedName(useBangla) ?: it }
                                        ?: stringResource(R.string.uncategorized),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(transaction.paymentMethod.localizedLabel(), style = MaterialTheme.typography.bodySmall)
                                Text(java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(MonthKey.DHAKA).toLocalDate().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0])), style = MaterialTheme.typography.bodySmall)
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
        var amount by remember(transaction.id) { mutableStateOf("${transaction.amountMinor / 100}.${(transaction.amountMinor % 100).toString().padStart(2, '0')}") }
        var type by remember(transaction.id) { mutableStateOf(transaction.type) }
        var categoryId by remember(transaction.id) { mutableStateOf(transaction.categoryId) }
        var paymentMethod by remember(transaction.id) { mutableStateOf(transaction.paymentMethod) }
        var note by remember(transaction.id) { mutableStateOf(transaction.note.orEmpty()) }
        var date by remember(transaction.id) { mutableStateOf(java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(com.hisabnikash.app.core.time.MonthKey.DHAKA).toLocalDate().toString()) }
        val parsedAmount = MoneyParser.parseToMinorUnits(amount)
        val parsedDate = DateInput.parse(date)
        val expectedType = if (type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME
        val validCategory = state.categories[categoryId]?.type == expectedType
        AlertDialog(
            onDismissRequest = { if (!busy) { editing = null; actionError = false } },
            title = { Text(stringResource(R.string.edit_transaction)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == TransactionType.EXPENSE, { if (type != TransactionType.EXPENSE) categoryId = null; type = TransactionType.EXPENSE }, enabled = !busy, label = { Text(stringResource(R.string.expense)) })
                    FilterChip(type == TransactionType.INCOME, { if (type != TransactionType.INCOME) categoryId = null; type = TransactionType.INCOME }, enabled = !busy, label = { Text(stringResource(R.string.income)) })
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.amount_bdt)) })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.description)) })
                CategoryDropdown(state.categories.values.filter { it.type == expectedType && (it.isActive || it.id == categoryId) }, categoryId, { categoryId = it }, null)
                PaymentMethodDropdown(paymentMethod, { paymentMethod = it })
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(stringResource(R.string.transaction_date)) })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.note_optional)) })
                if (actionError) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
            }
            },
            confirmButton = {
                Button(onClick = {
                    busy = true
                    scope.launch { try {
                    val saved = onUpdate(transaction.copy(
                        type = type,
                        amountMinor = (parsedAmount as MoneyParseResult.Success).minorUnits,
                        description = description.trim(),
                        categoryId = categoryId,
                        oneOffCategoryLabel = transaction.oneOffCategoryLabel.takeIf { categoryId == transaction.categoryId && type == transaction.type },
                        paymentMethod = paymentMethod,
                        note = note.trim().ifEmpty { null },
                        occurredAtEpochMillis = parsedDate!!.atStartOfDay(com.hisabnikash.app.core.time.MonthKey.DHAKA).toInstant().toEpochMilli(),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ))
                    if (saved) { editing = null; actionError = false } else actionError = true
                    } finally { busy = false } }
                }, enabled = !busy && description.isNotBlank() && validCategory && parsedAmount is MoneyParseResult.Success && parsedDate != null) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null; actionError = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }

    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; actionError = false } },
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = { Column { Text(stringResource(R.string.delete_transaction_message)); if (actionError) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error) } },
            confirmButton = {
                Button(onClick = {
                    busy = true
                    scope.launch { try { if (onDelete(transaction.id, true)) { deleting = null; actionError = false } else actionError = true } finally { busy = false } }
                }, enabled = !busy) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null; actionError = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
