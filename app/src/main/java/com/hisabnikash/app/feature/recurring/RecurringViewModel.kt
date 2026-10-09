package com.hisabnikash.app.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.model.RecurrenceFrequency
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.repository.RecurringRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

data class RecurringState(
    val items: List<RecurringItem> = emptyList(),
    val error: String? = null,
)

data class RecurringDraft(
    val type: TransactionType,
    val amountInput: String,
    val description: String,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val note: String,
    val dueDateIso: String,
    val quickConfirm: Boolean,
)

class RecurringViewModel(
    private val repository: RecurringRepository,
    private val scheduler: RecurringScheduler,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecurringState())
    val state: StateFlow<RecurringState> = mutableState
    private val actionMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { items ->
                mutableState.value = mutableState.value.copy(items = items.sortedBy(RecurringItem::nextDueAtEpochMillis))
                scheduler.rescheduleAll(items)
            }
        }
    }

    suspend fun save(item: RecurringItem) {
        repository.upsert(item)
        scheduler.schedule(item)
    }

    suspend fun create(draft: RecurringDraft): Boolean {
        val parsed = MoneyParser.parseToMinorUnits(draft.amountInput)
        val dueDate = com.hisabnikash.app.core.time.DateInput.parse(draft.dueDateIso)
        if (draft.description.isBlank() || draft.categoryId == null || parsed !is MoneyParseResult.Success || dueDate == null) return false
        val now = nowEpochMillis()
        val nextDue = dueDate.atStartOfDay(MonthKey.DHAKA).toInstant().toEpochMilli()
        if (nextDue <= now) return false
        return action {
          save(
            RecurringItem(
                id = newId(),
                type = draft.type,
                amountMinor = parsed.minorUnits,
                description = draft.description.trim(),
                categoryId = draft.categoryId,
                paymentMethod = draft.paymentMethod,
                note = draft.note.trim().ifEmpty { null },
                frequency = RecurrenceFrequency.MONTHLY,
                reminderStyle = if (draft.quickConfirm) ReminderStyle.QUICK_CONFIRM else ReminderStyle.OPEN_AND_REVIEW,
                nextDueAtEpochMillis = nextDue,
                createdAtEpochMillis = now,
            ),
          )
        }
    }

    suspend fun quickConfirm(id: String, expectedDueAt: Long = displayedDue(id)): Boolean = action {
        repository.confirmOccurrence(id, expectedDueAt)
        repository.find(id)?.let(scheduler::schedule)
    }

    suspend fun openForReview(id: String): RecurringEditor? = repository.find(id)?.let(RecurringEditor::from)

    suspend fun confirmWithEdits(id: String, editor: RecurringEditor, expectedDueAt: Long = displayedDue(id)): Boolean = action {
        repository.confirmOccurrence(id, expectedDueAt, editor)
        repository.find(id)?.let(scheduler::schedule)
    }

    suspend fun skip(id: String, expectedDueAt: Long = displayedDue(id)): Boolean = action {
        repository.skipOccurrence(id, expectedDueAt)
        repository.find(id)?.let(scheduler::schedule)
    }

    suspend fun disable(id: String): Boolean = action {
        repository.disable(id)
        scheduler.cancel(id)
    }

    private fun displayedDue(id: String) = state.value.items.firstOrNull { it.id == id }?.nextDueAtEpochMillis ?: Long.MIN_VALUE

    private suspend fun action(block: suspend () -> Unit): Boolean {
        if (!actionMutex.tryLock()) return false
        return try {
            block()
            mutableState.value = mutableState.value.copy(error = null)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(error = "operation_failed")
            false
        } finally { actionMutex.unlock() }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            action { repository.delete(id); scheduler.cancel(id) }
        }
    }
}
