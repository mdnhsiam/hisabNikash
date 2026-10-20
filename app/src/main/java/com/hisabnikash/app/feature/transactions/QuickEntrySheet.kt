package com.hisabnikash.app.feature.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntrySheet(
    state: QuickEntryState,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSubmit: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCustomCategoryChange: (String, Boolean) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
) {
    var touched by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val dirty by rememberUpdatedState(touched || state.amountInput.isNotEmpty() || state.description.isNotEmpty() ||
        state.note.isNotEmpty() || state.customCategoryLabel.isNotEmpty())
    val saving by rememberUpdatedState(state.isSaving)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = false, confirmValueChange = { target ->
        if (target == SheetValue.Hidden && (dirty || saving)) {
            if (!saving) confirmDiscard = true
            false
        } else true
    })
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val closeLabel = stringResource(R.string.close_entry)
    val resizeLabel = stringResource(if (sheet.currentValue == SheetValue.Expanded) R.string.collapse_entry else R.string.expand_entry)
    fun requestDismiss() {
        if (!saving) {
            if (dirty) confirmDiscard = true else onDismiss()
        }
    }
    ModalBottomSheet(onDismissRequest = ::requestDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.95f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.quick_entry), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = {
                    keyboard?.hide()
                    scope.launch {
                        if (sheet.currentValue == SheetValue.Expanded && sheet.hasPartiallyExpandedState) sheet.partialExpand()
                        else sheet.expand()
                    }
                }, modifier = Modifier.semantics { contentDescription = resizeLabel }) { Text("↕", style = MaterialTheme.typography.titleLarge) }
                IconButton(onClick = ::requestDismiss, enabled = !saving,
                    modifier = Modifier.semantics { contentDescription = closeLabel }) { Text("×", style = MaterialTheme.typography.titleLarge) }
            }
            QuickEntryScreen(
                state = state, onSubmit = onSubmit, showHeading = false, modifier = Modifier.weight(1f),
                onTypeChange = { touched = true; onTypeChange(it) },
                onAmountChange = { touched = true; onAmountChange(it) },
                onDescriptionChange = { touched = true; onDescriptionChange(it) },
                onCategoryChange = { touched = true; onCategoryChange(it) },
                onCustomCategoryChange = { label, save -> touched = true; onCustomCategoryChange(label, save) },
                onPaymentMethodChange = { touched = true; onPaymentMethodChange(it) },
                onNoteChange = { touched = true; onNoteChange(it) },
                onDateChange = { touched = true; onDateChange(it) },
                onDraftInteraction = { touched = true },
            )
        }
    }
    if (confirmDiscard) {
        AlertDialog(onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_entry_title)) },
            text = { Text(stringResource(R.string.discard_entry_message)) },
            confirmButton = { TextButton(onClick = {
                if (!saving) { onDiscard(); confirmDiscard = false; onDismiss() }
            }, enabled = !saving) { Text(stringResource(R.string.discard_entry)) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } })
    }
}
