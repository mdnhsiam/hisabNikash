package com.hisabnikash.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId"), Index("allocatedAtEpochMillis"), Index(value = ["reversesAllocationId"], unique = true)],
)
data class GoalAllocationEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val amountMinor: Long,
    val note: String?,
    val allocatedAtEpochMillis: Long,
    val reversesAllocationId: String?,
)
