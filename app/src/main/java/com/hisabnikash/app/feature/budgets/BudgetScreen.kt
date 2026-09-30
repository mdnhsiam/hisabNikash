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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyFormatter
import com.hisabnikash.app.ui.components.CategoryDropdown

@Composable
fun BudgetScreen(
    state: BudgetState,
    onSave: (String, String?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.monthly_budget), style = MaterialTheme.typography.titleMedium)
            if (!state.isConfigured) Text(stringResource(R.string.budget_setup_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.totalBudget?.let { budget ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.total_budget))
                    Text(MoneyFormatter.formatMinorUnits(budget.amountMinor))
                    TextButton(onClick = { onDelete(budget.id) }) { Text(stringResource(R.string.delete)) }
                }
            }
            state.categoryBudgets.forEach { budget ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.categories.firstOrNull { it.id == budget.categoryId }?.nameEn ?: budget.categoryId.orEmpty())
                    Text(MoneyFormatter.formatMinorUnits(budget.amountMinor))
                    TextButton(onClick = { onDelete(budget.id) }) { Text(stringResource(R.string.delete)) }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.budget_amount)) },
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryDropdown(
                categories = state.categories,
                selectedId = categoryId,
                onSelected = { categoryId = it },
                error = null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(amount, null); amount = "" }) { Text(stringResource(R.string.save_total_budget)) }
                Button(onClick = { onSave(amount, categoryId); amount = "" }, enabled = categoryId != null) {
                    Text(stringResource(R.string.save_category_budget))
                }
            }
        }
    }
}
