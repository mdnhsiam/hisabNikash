package com.hisabnikash.app.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.SavingsGoal
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.data.repository.SavingsRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SavingsState(
    val monthlySavingsMinor: Long = 0,
    val goals: List<SavingsGoal> = emptyList(),
    val allocations: List<GoalAllocation> = emptyList(),
) {
    val allocatedMinor: Long get() = allocations.sumOf(GoalAllocation::amountMinor)
    val availableMinor: Long get() = monthlySavingsMinor - allocatedMinor
    fun progressFor(goalId: String): Long = allocations.filter { it.goalId == goalId }.sumOf(GoalAllocation::amountMinor)
}

sealed interface SavingsActionResult {
    data object Saved : SavingsActionResult
    data object InvalidInput : SavingsActionResult
    data object InsufficientSavings : SavingsActionResult
    data class Failed(val message: String) : SavingsActionResult
}

class SavingsViewModel(
    private val repository: SavingsRepository,
    monthlySavingsMinor: Flow<Long>,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(SavingsState())
    val state: StateFlow<SavingsState> = mutableState

    init {
        viewModelScope.launch {
            combine(monthlySavingsMinor, repository.observeGoals(), repository.observeAllocations()) { savings, goals, allocations ->
                SavingsState(savings, goals, allocations)
            }.collect(mutableState)
        }
    }

    suspend fun createGoal(name: String, targetInput: String): SavingsActionResult {
        if (name.isBlank()) return SavingsActionResult.InvalidInput
        val target = if (targetInput.isBlank()) null else when (val parsed = MoneyParser.parseToMinorUnits(targetInput)) {
            is MoneyParseResult.Success -> parsed.minorUnits
            is MoneyParseResult.Error -> return SavingsActionResult.InvalidInput
        }
        val now = nowEpochMillis()
        return runCatching {
            repository.upsertGoal(SavingsGoal(newId(), name.trim(), target, createdAtEpochMillis = now))
        }.fold({ SavingsActionResult.Saved }, { SavingsActionResult.Failed(it.message ?: "Could not save goal") })
    }

    suspend fun allocate(goalId: String, amountInput: String): SavingsActionResult {
        val parsed = MoneyParser.parseToMinorUnits(amountInput)
        if (parsed !is MoneyParseResult.Success) return SavingsActionResult.InvalidInput
        val available = mutableState.value.availableMinor.coerceAtLeast(0)
        if (parsed.minorUnits > available) return SavingsActionResult.InsufficientSavings
        val allocation = GoalAllocation(newId(), goalId, parsed.minorUnits, allocatedAtEpochMillis = nowEpochMillis())
        return runCatching { repository.allocate(allocation) }
            .fold({ SavingsActionResult.Saved }, { SavingsActionResult.Failed(it.message ?: "Could not allocate savings") })
    }

    suspend fun reverse(allocationId: String): SavingsActionResult =
        runCatching { repository.reverse(allocationId, newId(), nowEpochMillis()) }
            .fold({ SavingsActionResult.Saved }, { SavingsActionResult.Failed(it.message ?: "Could not reverse allocation") })

    fun archiveGoal(id: String) {
        viewModelScope.launch { repository.archiveGoal(id, nowEpochMillis()) }
    }
}
