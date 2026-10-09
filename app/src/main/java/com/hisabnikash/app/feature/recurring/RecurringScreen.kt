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
import com.hisabnikash.app.core.time.DateInput
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.money.MoneyParseResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

@Composable
fun RecurringScreen(
    state: RecurringState,
    modifier: Modifier = Modifier,
    requestedReviewId: String? = null,
    requestedReviewDueAt: Long? = null,
    onReviewRequestConsumed: () -> Unit = {},
    categories: List<Category> = emptyList(),
    notificationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCreate: suspend (RecurringDraft) -> Boolean,
    onConfirm: suspend (String, Long) -> Boolean,
    onReviewConfirm: suspend (String, Long, RecurringEditor) -> Boolean,
    onSkip: suspend (String, Long) -> Boolean,
    onDisable: suspend (String) -> Boolean,
) {
    var showEditor by remember { mutableStateOf(false) }
    var reviewItem by remember { mutableStateOf<RecurringItem?>(null) }
    var editorError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(requestedReviewId, state.items) {
        requestedReviewId?.let { id ->
            state.items.firstOrNull { it.id == id }?.let {
                if (it.isEnabled && (requestedReviewDueAt == null || it.nextDueAtEpochMillis == requestedReviewDueAt)) reviewItem = it
                else editorError = true
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
        if (state.error != null || editorError) Text(stringResource(R.string.recurring_action_failed), color = MaterialTheme.colorScheme.error)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.items.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.description, style = MaterialTheme.typography.titleMedium)
                        Text(MoneyFormatter.formatMinorUnits(item.amountMinor))
                        Text(java.time.Instant.ofEpochMilli(item.nextDueAtEpochMillis).atZone(MonthKey.DHAKA).toLocalDate().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])))
                        Text(stringResource(if (!item.isEnabled) R.string.recurring_disabled else if (item.type == TransactionType.EXPENSE) R.string.expense else R.string.income), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { if (item.reminderStyle == ReminderStyle.QUICK_CONFIRM) {
                                busy = true; scope.launch { try { editorError = !onConfirm(item.id, item.nextDueAtEpochMillis) } finally { busy = false } }
                            } else { reviewItem = item; editorError = false } }, enabled = item.isEnabled && !busy) {
                                Text(if (item.reminderStyle == ReminderStyle.QUICK_CONFIRM) stringResource(R.string.confirm) else stringResource(R.string.open_review))
                            }
                            TextButton(onClick = { busy = true; scope.launch { try { editorError = !onSkip(item.id, item.nextDueAtEpochMillis) } finally { busy = false } } }, enabled = item.isEnabled && !busy) { Text(stringResource(R.string.skip)) }
                            TextButton(onClick = { busy = true; scope.launch { try { editorError = !onDisable(item.id) } finally { busy = false } } }, enabled = item.isEnabled && !busy) { Text(stringResource(R.string.disable)) }
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
            onDismissRequest = { if (!busy) showEditor = false },
            title = { Text(stringResource(R.string.add_recurring)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(Modifier.toggleable(quick, role = Role.Checkbox, onValueChange = { quick = it })) { Checkbox(quick, null); Text(stringResource(R.string.quick_confirm)) }
                    if (editorError) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { Button(onClick = { busy = true; scope.launch { try { if (onCreate(RecurringDraft(type, amount, description, categoryId, paymentMethod, note, dueDate, quick))) { showEditor = false; editorError = false } else editorError = true } finally { busy = false } } }, enabled = !busy) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showEditor = false; editorError = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }

    reviewItem?.let { item ->
        var description by remember(item.id) { mutableStateOf(item.description) }
        var amount by remember(item.id) { mutableStateOf("${item.amountMinor / 100}.${(item.amountMinor % 100).toString().padStart(2, '0')}") }
        var categoryId by remember(item.id) { mutableStateOf(item.categoryId) }
        var paymentMethod by remember(item.id) { mutableStateOf(item.paymentMethod) }
        var note by remember(item.id) { mutableStateOf(item.note.orEmpty()) }
        val parsed = MoneyParser.parseToMinorUnits(amount)
        val matchingCategories = categories.filter { it.type == (if (item.type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME) && (it.isActive || it.id == categoryId) }
        AlertDialog(
            onDismissRequest = { if (!busy) reviewItem = null },
            title = { Text(stringResource(R.string.open_review)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
                OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount_bdt)) })
                CategoryDropdown(matchingCategories, categoryId, { categoryId = it }, null)
                PaymentMethodDropdown(paymentMethod, { paymentMethod = it })
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note_optional)) })
                if (editorError) Text(stringResource(R.string.recurring_action_failed), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = {
                Button(onClick = {
                    busy = true
                    val editor = RecurringEditor.from(item).copy(description = description.trim(), amountMinor = (parsed as MoneyParseResult.Success).minorUnits,
                        categoryId = categoryId, paymentMethod = paymentMethod, note = note.trim().ifEmpty { null })
                    scope.launch { try { if (onReviewConfirm(item.id, item.nextDueAtEpochMillis, editor)) { reviewItem = null; editorError = false } else editorError = true } finally { busy = false } }
                }, enabled = !busy && description.isNotBlank() && parsed is MoneyParseResult.Success && matchingCategories.any { it.id == categoryId }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { reviewItem = null; editorError = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
