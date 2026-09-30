package com.hisabnikash.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val query: String = "",
    val error: String? = null,
) {
    val visibleTransactions: List<Transaction>
        get() = transactions.filter { transaction ->
            query.isBlank() || transaction.description.contains(query.trim(), ignoreCase = true) ||
                transaction.note.orEmpty().contains(query.trim(), ignoreCase = true)
        }
}

class TransactionListViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { transactions ->
                mutableState.update { it.copy(transactions = transactions.sortedByDescending(Transaction::occurredAtEpochMillis)) }
            }
        }
    }

    fun setQuery(query: String) = mutableState.update { it.copy(query = query) }

    fun update(transaction: Transaction) {
        viewModelScope.launch {
            runCatching { repository.update(transaction) }
                .onFailure { error -> mutableState.update { it.copy(error = error.message) } }
        }
    }

    fun delete(id: String, confirmed: Boolean) {
        if (!confirmed) return
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { error -> mutableState.update { it.copy(error = error.message) } }
        }
    }
}
