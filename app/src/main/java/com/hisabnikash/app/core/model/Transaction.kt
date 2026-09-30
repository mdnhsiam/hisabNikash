package com.hisabnikash.app.core.model

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val categoryId: String?,
    val oneOffCategoryLabel: String? = null,
    val paymentMethod: PaymentMethod,
    val note: String? = null,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)
