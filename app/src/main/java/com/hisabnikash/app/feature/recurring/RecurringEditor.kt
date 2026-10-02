package com.hisabnikash.app.feature.recurring

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType

data class RecurringEditor(
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val note: String?,
) {
    companion object {
        fun from(item: RecurringItem) = RecurringEditor(item.type, item.amountMinor, item.description, item.categoryId, item.paymentMethod, item.note)
    }
}

fun RecurringItem.toTransaction(
    edits: RecurringEditor?,
    createdAtEpochMillis: Long,
    id: String = "recurring-${this.id}-$createdAtEpochMillis",
): Transaction {
    val values = edits ?: RecurringEditor.from(this)
    return Transaction(
        id = id,
        type = values.type,
        amountMinor = values.amountMinor,
        description = values.description,
        categoryId = values.categoryId,
        paymentMethod = values.paymentMethod,
        note = values.note,
        occurredAtEpochMillis = createdAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
