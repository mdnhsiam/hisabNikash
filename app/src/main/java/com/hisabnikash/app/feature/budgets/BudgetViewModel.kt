package com.hisabnikash.app.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.BudgetRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class BudgetState(
    val month: MonthKey,
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null,
) {
    val totalBudget: Budget? get() = budgets.firstOrNull { it.categoryId == null }
    val categoryBudgets: List<Budget> get() = budgets.filter { it.categoryId != null }
    val isConfigured: Boolean get() = budgets.isNotEmpty()
}

sealed interface BudgetSaveResult {
    data object Saved : BudgetSaveResult
    data object InvalidAmount : BudgetSaveResult
    data class Failed(val message: String) : BudgetSaveResult
}

class BudgetViewModel(
    private val repository: BudgetRepository,
    private val month: MonthKey,
    categories: Flow<List<Category>> = flowOf(emptyList()),
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(BudgetState(month))
    val state: StateFlow<BudgetState> = mutableState

    init {
        viewModelScope.launch {
            combine(repository.observeMonth(month), categories) { budgets, categoryList -> budgets to categoryList }
                .collect { (budgets, categoryList) -> mutableState.update { it.copy(budgets = budgets, categories = categoryList) } }
        }
    }

    suspend fun saveBudget(amountInput: String, categoryId: String?): BudgetSaveResult {
        val parsed = MoneyParser.parseToMinorUnits(amountInput)
        if (parsed !is MoneyParseResult.Success) return BudgetSaveResult.InvalidAmount
        val existing = mutableState.value.budgets.firstOrNull { it.categoryId == categoryId }
        val now = nowEpochMillis()
        val budget = Budget(
            id = existing?.id ?: newId(),
            monthKey = month.value,
            categoryId = categoryId,
            amountMinor = parsed.minorUnits,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )
        return runCatching { repository.upsert(budget) }
            .fold(onSuccess = { BudgetSaveResult.Saved }, onFailure = { BudgetSaveResult.Failed(it.message ?: "Could not save budget") })
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
