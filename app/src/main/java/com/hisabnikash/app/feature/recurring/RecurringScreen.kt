package com.hisabnikash.app.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.ui.components.CategoryDropdown
import com.hisabnikash.app.ui.components.PaymentMethodDropdown
import androidx.compose.material3.FilterChip
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.core.time.MonthKey

@Composable
fun RecurringScreen(
    state: RecurringState,
    requestedReviewId: String? = null,
    onReviewRequestConsumed: () -> Unit = {},
    categories: List<Category> = emptyList(),
    notificationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCreate: suspend (RecurringDraft) -> Boolean,
    onConfirm: (String) -> Unit,
    onReviewConfirm: (String, RecurringEditor) -> Unit,
    onSkip: (String) -> Unit,
    onDisable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }
    var reviewItem by remember { mutableStateOf<RecurringItem?>(null) }
    var editorError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(requestedReviewId, state.items) {
        requestedReviewId?.let { id ->
            state.items.firstOrNull { it.id == id }?.let {
                reviewItem = it
                onReviewRequestConsumed()
            }
        }
    }
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.recurring_title), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showEditor = true }) { Text(stringResource(R.string.add_recurring)) }
        }
        if (!notificationPermissionGranted) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.notification_permission_help))
                    TextButton(onClick = onRequestPermission) { Text(stringResource(R.string.allow_notifications)) }
                }
            }
        }
        if (state.items.isEmpty()) Text(stringResource(R.string.no_recurring), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.items.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.description, style = MaterialTheme.typography.titleMedium)
                        Text(MoneyFormatter.formatMinorUnits(item.amountMinor))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { if (item.reminderStyle == ReminderStyle.QUICK_CONFIRM) onConfirm(item.id) else reviewItem = item }) {
                                Text(if (item.reminderStyle == ReminderStyle.QUICK_CONFIRM) stringResource(R.string.confirm) else stringResource(R.string.open_review))
                            }
                            TextButton(onClick = { onSkip(item.id) }) { Text(stringResource(R.string.skip)) }
                            TextButton(onClick = { onDisable(item.id) }) { Text(stringResource(R.string.disable)) }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var description by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var quick by remember { mutableStateOf(false) }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }
        var categoryId by remember(type, categories) { mutableStateOf(categories.firstOrNull { it.type == (if (type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME) && it.isActive }?.id) }
        var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
        var note by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf(java.time.LocalDate.now(MonthKey.DHAKA).plusMonths(1).toString()) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(stringResource(R.string.add_recurring)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(type == TransactionType.EXPENSE, { type = TransactionType.EXPENSE }, label = { Text(stringResource(R.string.expense)) })
                        FilterChip(type == TransactionType.INCOME, { type = TransactionType.INCOME }, label = { Text(stringResource(R.string.income)) })
                    }
                    OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
                    OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount_bdt)) })
                    CategoryDropdown(categories.filter { it.type == (if (type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME) && it.isActive }, categoryId, { categoryId = it }, null, Modifier.fillMaxWidth())
                    PaymentMethodDropdown(paymentMethod, { paymentMethod = it })
                    OutlinedTextField(dueDate, { dueDate = it }, label = { Text(stringResource(R.string.next_due_date)) })
                    OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note_optional)) })
                    Text(stringResource(R.string.frequency_monthly), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row { Checkbox(quick, { quick = it }); Text(stringResource(R.string.quick_confirm)) }
                    if (editorError) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { Button(onClick = { scope.launch { if (onCreate(RecurringDraft(type, amount, description, categoryId, paymentMethod, note, dueDate, quick))) { showEditor = false; editorError = false } else editorError = true } }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    reviewItem?.let { item ->
        var description by remember(item.id) { mutableStateOf(item.description) }
        AlertDialog(
            onDismissRequest = { reviewItem = null },
            title = { Text(stringResource(R.string.open_review)) },
            text = { OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) }) },
            confirmButton = {
                Button(onClick = {
                    onReviewConfirm(item.id, RecurringEditor.from(item).copy(description = description))
                    reviewItem = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { reviewItem = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
