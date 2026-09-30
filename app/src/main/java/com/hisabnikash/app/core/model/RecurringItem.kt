package com.hisabnikash.app.core.model

data class RecurringItem(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val note: String? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val reminderStyle: ReminderStyle = ReminderStyle.OPEN_AND_REVIEW,
    val nextDueAtEpochMillis: Long,
    val isEnabled: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)
