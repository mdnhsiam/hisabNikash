package com.hisabnikash.app.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.CategoryDropdown
import com.hisabnikash.app.ui.components.localizedName

@Composable
fun BudgetScreen(
    state: BudgetState,
    onSave: suspend (String, String?) -> BudgetSaveResult,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val useBangla = LocalConfiguration.current.locales[0].language == "bn"
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.monthly_budget), style = MaterialTheme.typography.titleMedium)
            if (!state.isConfigured) Text(stringResource(R.string.budget_setup_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.totalBudget?.let { budget ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.total_budget), Modifier.weight(1f))
                    Text(MoneyFormatter.formatMinorUnits(budget.amountMinor), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onDelete(budget.id) }) { Text(stringResource(R.string.delete)) }
                }
            }
            state.categoryBudgets.forEach { budget ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.categories.firstOrNull { it.id == budget.categoryId }?.localizedName(useBangla) ?: budget.categoryId.orEmpty(), Modifier.weight(1f), maxLines = 2)
                    Text(MoneyFormatter.formatMinorUnits(budget.amountMinor), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onDelete(budget.id) }) { Text(stringResource(R.string.delete)) }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.budget_amount)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            CategoryDropdown(
                categories = state.categories,
                selectedId = categoryId,
                onSelected = { categoryId = it },
                error = null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { busy = true; scope.launch { try { if (onSave(amount, null) is BudgetSaveResult.Saved) { amount = ""; errorRes = null } else errorRes = R.string.operation_failed } finally { busy = false } } }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save_total_budget)) }
                Button(onClick = { busy = true; scope.launch { try { if (onSave(amount, categoryId) is BudgetSaveResult.Saved) { amount = ""; errorRes = null } else errorRes = R.string.operation_failed } finally { busy = false } } }, enabled = !busy && categoryId != null, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.save_category_budget))
                }
            }
            errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            if (state.error != null) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
