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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException

data class SavingsState(
    val monthlySavingsMinor: Long = 0,
    val goals: List<SavingsGoal> = emptyList(),
    val allocations: List<GoalAllocation> = emptyList(),
    val lifetimeSavingsMinor: Long = monthlySavingsMinor,
) {
    val allocatedMinor: Long get() = allocations.sumOf(GoalAllocation::amountMinor)
    val availableMinor: Long get() = lifetimeSavingsMinor - allocatedMinor
    fun progressFor(goalId: String): Long = allocations.filter { it.goalId == goalId }.sumOf(GoalAllocation::amountMinor)
}

sealed interface SavingsActionResult {
    data object Saved : SavingsActionResult
    data object InvalidInput : SavingsActionResult
    data object InsufficientSavings : SavingsActionResult
    data object InProgress : SavingsActionResult
    data class Failed(val message: String) : SavingsActionResult
}

class SavingsViewModel(
    private val repository: SavingsRepository,
    monthlySavingsMinor: Flow<Long>,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    lifetimeSavingsMinor: Flow<Long> = monthlySavingsMinor,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SavingsState())
    val state: StateFlow<SavingsState> = mutableState
    private val actionMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(monthlySavingsMinor, repository.observeGoals(), repository.observeAllocations(), lifetimeSavingsMinor) { savings, goals, allocations, lifetime ->
                SavingsState(savings, goals, allocations, lifetime)
            }.collect(mutableState)
        }
    }

    suspend fun createGoal(name: String, targetInput: String): SavingsActionResult = guarded {
        if (name.isBlank()) return@guarded SavingsActionResult.InvalidInput
        val target = if (targetInput.isBlank()) null else when (val parsed = MoneyParser.parseToMinorUnits(targetInput)) {
            is MoneyParseResult.Success -> parsed.minorUnits
            is MoneyParseResult.Error -> return@guarded SavingsActionResult.InvalidInput
        }
        val now = nowEpochMillis()
        repository.upsertGoal(SavingsGoal(newId(), name.trim(), target, createdAtEpochMillis = now))
        SavingsActionResult.Saved
    }

    suspend fun allocate(goalId: String, amountInput: String): SavingsActionResult = guarded {
        val parsed = MoneyParser.parseToMinorUnits(amountInput)
        if (parsed !is MoneyParseResult.Success) return@guarded SavingsActionResult.InvalidInput
        val available = mutableState.value.availableMinor.coerceAtLeast(0)
        if (parsed.minorUnits > available) return@guarded SavingsActionResult.InsufficientSavings
        val allocation = GoalAllocation(newId(), goalId, parsed.minorUnits, allocatedAtEpochMillis = nowEpochMillis())
        repository.allocate(allocation)
        SavingsActionResult.Saved
    }

    suspend fun reverse(allocationId: String): SavingsActionResult = guarded {
        repository.reverse(allocationId, newId(), nowEpochMillis())
        SavingsActionResult.Saved
    }

    private suspend fun guarded(block: suspend () -> SavingsActionResult): SavingsActionResult {
        if (!actionMutex.tryLock()) return SavingsActionResult.InProgress
        return try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { SavingsActionResult.Failed("operation_failed") }
        finally { actionMutex.unlock() }
    }

    fun archiveGoal(id: String) {
        viewModelScope.launch { guarded { repository.archiveGoal(id, nowEpochMillis()); SavingsActionResult.Saved } }
    }
}
