package com.hisabnikash.app.core.model

data class Budget(
    val id: String,
    val monthKey: String,
    val categoryId: String?,
    val amountMinor: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)
