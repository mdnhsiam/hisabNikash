package com.hisabnikash.app.feature.recurring

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.money.MoneyFormatter

@Composable
fun RecurringScreen(
    state: RecurringState,
    notificationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCreate: (String, String, Boolean) -> Unit,
    onConfirm: (String) -> Unit,
    onReviewConfirm: (String, RecurringEditor) -> Unit,
    onSkip: (String) -> Unit,
    onDisable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }
    var reviewItem by remember { mutableStateOf<RecurringItem?>(null) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = RecurringItem::id) { item ->
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
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(stringResource(R.string.add_recurring)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
                    OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount_bdt)) })
                    Row { Checkbox(quick, { quick = it }); Text(stringResource(R.string.quick_confirm)) }
                }
            },
            confirmButton = { Button(onClick = { onCreate(description, amount, quick); showEditor = false }) { Text(stringResource(R.string.save)) } },
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
