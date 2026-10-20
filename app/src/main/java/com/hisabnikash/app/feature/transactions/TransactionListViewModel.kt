package com.hisabnikash.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.data.repository.TransactionRepository
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val categories: Map<String, Category> = emptyMap(),
    val typeFilter: TransactionType? = null,
    val monthFilter: MonthKey? = null,
) {
    val visibleTransactions: List<Transaction>
        get() = transactions.filter { transaction ->
            (typeFilter == null || transaction.type == typeFilter) &&
                (monthFilter == null || transaction.occurredAtEpochMillis in monthFilter.startEpochMillis() until monthFilter.endExclusiveEpochMillis()) &&
                (query.isBlank() || transaction.description.contains(query.trim(), ignoreCase = true) ||
                transaction.note.orEmpty().contains(query.trim(), ignoreCase = true))
        }
}

class TransactionListViewModel(
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), categoryRepository?.observeAll() ?: flowOf(emptyList())) { transactions, categories -> transactions to categories }.collect { (transactions, categories) ->
                mutableState.update { it.copy(transactions = transactions.sortedByDescending(Transaction::occurredAtEpochMillis), categories = categories.associateBy { category -> category.id }) }
            }
        }
    }

    fun setQuery(query: String) = mutableState.update { it.copy(query = query) }
    fun setDashboardFilter(type: TransactionType?, month: MonthKey?) = mutableState.update {
        it.copy(typeFilter = type, monthFilter = month, query = "")
    }

    suspend fun update(transaction: Transaction): Boolean = action { repository.update(transaction) }

    private suspend fun action(block: suspend () -> Unit): Boolean = try {
        block()
        mutableState.update { it.copy(error = null) }
        true
    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
    } catch (_: Exception) {
        mutableState.update { it.copy(error = "operation_failed") }
        false
    }

    suspend fun delete(id: String, confirmed: Boolean): Boolean {
        if (!confirmed) return false
        return action { repository.delete(id) }
    }
}
