package com.hisabnikash.app.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.TransactionRepository
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.domain.FinanceCalculator
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReportsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    initialMonth: MonthKey = MonthKey.fromInstant(Instant.now()),
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(initialMonth)
    private val mutableState = MutableStateFlow(ReportsState(initialMonth, emptyMonthlyReport(initialMonth)))
    val state: StateFlow<ReportsState> = mutableState

    init {
        viewModelScope.launch {
            combine(transactionRepository.observeAll(), selectedMonth, categoryRepository.observeAll()) { transactions, month, categories ->
                ReportsState(month, FinanceCalculator.monthlyReport(transactions, month), categories.associateBy { it.id })
            }.collect(mutableState)
        }
    }

    fun previousMonth() { selectedMonth.value = selectedMonth.value.previous() }
    fun nextMonth() { selectedMonth.value = selectedMonth.value.next() }
}
