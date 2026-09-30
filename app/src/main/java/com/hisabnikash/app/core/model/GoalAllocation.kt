package com.hisabnikash.app.core.model

data class GoalAllocation(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val note: String? = null,
    val allocatedAtEpochMillis: Long,
)
