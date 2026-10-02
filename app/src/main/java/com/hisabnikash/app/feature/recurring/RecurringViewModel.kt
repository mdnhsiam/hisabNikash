package com.hisabnikash.app.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.RecurringRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecurringState(
    val items: List<RecurringItem> = emptyList(),
    val error: String? = null,
)

class RecurringViewModel(
    private val repository: RecurringRepository,
    private val scheduler: RecurringScheduler,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecurringState())
    val state: StateFlow<RecurringState> = mutableState

    init {
        viewModelScope.launch {
            repository.observeAll().collect { items ->
                mutableState.value = RecurringState(items.sortedBy(RecurringItem::nextDueAtEpochMillis))
                scheduler.rescheduleAll(items)
            }
        }
    }

    suspend fun save(item: RecurringItem) {
        repository.upsert(item)
        scheduler.schedule(item)
    }

    suspend fun create(description: String, amountInput: String, quickConfirm: Boolean): Boolean {
        val parsed = MoneyParser.parseToMinorUnits(amountInput)
        if (description.isBlank() || parsed !is MoneyParseResult.Success) return false
        val now = nowEpochMillis()
        val nextDue = Instant.ofEpochMilli(now).atZone(MonthKey.DHAKA).plusMonths(1).toInstant().toEpochMilli()
        save(
            RecurringItem(
                id = newId(),
                type = TransactionType.EXPENSE,
                amountMinor = parsed.minorUnits,
                description = description.trim(),
                categoryId = "expense_bills",
                paymentMethod = PaymentMethod.BKASH,
                reminderStyle = if (quickConfirm) ReminderStyle.QUICK_CONFIRM else ReminderStyle.OPEN_AND_REVIEW,
                nextDueAtEpochMillis = nextDue,
                createdAtEpochMillis = now,
            ),
        )
        return true
    }

    suspend fun quickConfirm(id: String): Transaction {
        val transaction = repository.confirmOccurrence(id)
        repository.find(id)?.let(scheduler::schedule)
        return transaction
    }

    suspend fun openForReview(id: String): RecurringEditor? = repository.find(id)?.let(RecurringEditor::from)

    suspend fun confirmWithEdits(id: String, editor: RecurringEditor): Transaction {
        val transaction = repository.confirmOccurrence(id, editor)
        repository.find(id)?.let(scheduler::schedule)
        return transaction
    }

    suspend fun skip(id: String) {
        repository.skipOccurrence(id)
        repository.find(id)?.let(scheduler::schedule)
    }

    suspend fun disable(id: String) {
        repository.disable(id)
        scheduler.cancel(id)
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            scheduler.cancel(id)
        }
    }
}
