package com.hisabnikash.app.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.ui.components.AmountField
import com.hisabnikash.app.ui.components.CategoryDropdown
import com.hisabnikash.app.ui.components.PaymentMethodDropdown

@Composable
fun QuickEntryScreen(
    state: QuickEntryState,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onTypeChange: (TransactionType) -> Unit = {},
    onAmountChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onCategoryChange: (String) -> Unit = {},
    onCustomCategoryChange: (String, Boolean) -> Unit = { _, _ -> },
    onPaymentMethodChange: (PaymentMethod) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.quick_entry), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.type == TransactionType.EXPENSE,
                onClick = { onTypeChange(TransactionType.EXPENSE) },
                label = { Text(stringResource(R.string.expense)) },
            )
            FilterChip(
                selected = state.type == TransactionType.INCOME,
                onClick = { onTypeChange(TransactionType.INCOME) },
                label = { Text(stringResource(R.string.income)) },
            )
        }
        AmountField(
            value = state.amountInput,
            onValueChange = onAmountChange,
            label = stringResource(R.string.amount_bdt),
            error = state.errors[FormField.AMOUNT],
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.description)) },
            singleLine = true,
            isError = state.errors.containsKey(FormField.DESCRIPTION),
            supportingText = state.errors[FormField.DESCRIPTION]?.let { { Text(it) } },
        )
        CategoryDropdown(
            categories = state.categories,
            selectedId = state.categoryId,
            onSelected = onCategoryChange,
            error = state.errors[FormField.CATEGORY],
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.categoryId?.endsWith("_other") == true) {
            OutlinedTextField(
                value = state.customCategoryLabel,
                onValueChange = { onCustomCategoryChange(it, state.saveCustomCategory) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.custom_category)) },
                isError = state.errors.containsKey(FormField.CUSTOM_CATEGORY),
                supportingText = state.errors[FormField.CUSTOM_CATEGORY]?.let { { Text(it) } },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.saveCustomCategory,
                    onCheckedChange = { onCustomCategoryChange(state.customCategoryLabel, it) },
                )
                Text(stringResource(R.string.save_category_for_later))
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.payment_method), style = MaterialTheme.typography.labelLarge)
        PaymentMethodDropdown(selected = state.paymentMethod, onSelected = onPaymentMethodChange)
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.note_optional)) },
            minLines = 2,
        )
        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onSubmit,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.save_transaction))
        }
    }
}
