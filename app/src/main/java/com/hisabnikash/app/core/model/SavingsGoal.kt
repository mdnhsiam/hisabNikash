package com.hisabnikash.app.core.model

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetMinor: Long?,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)
