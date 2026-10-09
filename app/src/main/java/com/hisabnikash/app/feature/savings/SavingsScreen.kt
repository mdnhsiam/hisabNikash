package com.hisabnikash.app.feature.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.SavingsGoal
import com.hisabnikash.app.core.money.MoneyFormatter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun SavingsScreen(
    state: SavingsState,
    onCreateGoal: suspend (String, String) -> SavingsActionResult,
    onAllocate: suspend (String, String) -> SavingsActionResult,
    onReverse: suspend (String) -> SavingsActionResult,
    modifier: Modifier = Modifier,
) {
    var showGoalEditor by remember { mutableStateOf(false) }
    var allocatingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (state.monthlySavingsMinor < 0) stringResource(R.string.monthly_deficit) else stringResource(R.string.monthly_savings))
                Text(
                    MoneyFormatter.formatMinorUnits(state.monthlySavingsMinor),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.monthlySavingsMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.available_for_goals, MoneyFormatter.formatMinorUnits(state.availableMinor)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.savings_goals), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showGoalEditor = true }) { Text(stringResource(R.string.add_goal)) }
        }
        if (state.goals.isEmpty()) Text(stringResource(R.string.goal_setup_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.goals.forEach { goal ->
            val progress = state.progressFor(goal.id)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium)
                    Text(MoneyFormatter.formatMinorUnits(progress))
                    goal.targetMinor?.let { target ->
                        LinearProgressIndicator(progress = { (progress.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = { allocatingGoal = goal; errorRes = null }, enabled = !busy) { Text(stringResource(R.string.allocate_savings)) }
                    state.allocations.lastOrNull { it.goalId == goal.id && it.amountMinor > 0 && state.allocations.none { reversal -> reversal.reversesAllocationId == it.id } }?.let { allocation ->
                        TextButton(onClick = { busy = true; scope.launch { try { if (onReverse(allocation.id) !is SavingsActionResult.Saved) errorRes = R.string.operation_failed else errorRes = null } finally { busy = false } } }, enabled = !busy) { Text(stringResource(R.string.reverse_last_allocation)) }
                    }
                }
            }
        }
        errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }

    if (showGoalEditor) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) showGoalEditor = false },
            title = { Text(stringResource(R.string.add_goal)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.goal_name)) })
                    OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.target_optional)) })
                    errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { Button(onClick = { busy = true; scope.launch { try { if (onCreateGoal(name, target) is SavingsActionResult.Saved) { showGoalEditor = false; errorRes = null } else errorRes = R.string.operation_failed } finally { busy = false } } }, enabled = !busy && name.isNotBlank()) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showGoalEditor = false; errorRes = null }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }

    allocatingGoal?.let { goal ->
        var amount by remember(goal.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) allocatingGoal = null },
            title = { Text(stringResource(R.string.allocate_to_goal, goal.name)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount_bdt)) }); errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { Button(onClick = { busy = true; scope.launch { try { if (onAllocate(goal.id, amount) is SavingsActionResult.Saved) { allocatingGoal = null; errorRes = null } else errorRes = R.string.insufficient_or_invalid_savings } finally { busy = false } } }, enabled = !busy) { Text(stringResource(R.string.allocate_savings)) } },
            dismissButton = { TextButton(onClick = { allocatingGoal = null; errorRes = null }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
