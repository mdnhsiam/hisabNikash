package com.hisabnikash.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.TransactionRepository
import com.hisabnikash.app.domain.FinanceCalculator
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class DashboardViewModel(
    transactionRepository: TransactionRepository,
    private val currentMonth: () -> MonthKey = { MonthKey.fromInstant(Instant.now()) },
    budgets: Flow<List<Budget>> = flowOf(emptyList()),
    allocations: Flow<List<GoalAllocation>> = flowOf(emptyList()),
    months: Flow<MonthKey> = flowOf(currentMonth()),
    categories: Flow<List<com.hisabnikash.app.core.model.Category>> = flowOf(emptyList()),
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardState(emptyDashboardSummary(currentMonth())))
    val state: StateFlow<DashboardState> = mutableState

    init {
        viewModelScope.launch {
            combine(transactionRepository.observeAll(), budgets, allocations, months, categories) { transactions, currentBudgets, currentAllocations, month, categoryList ->
                DashboardState(FinanceCalculator.dashboard(transactions, currentBudgets, currentAllocations, month), categoryList.associateBy { it.id })
            }.collect(mutableState)
        }
    }
}
