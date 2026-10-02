package com.hisabnikash.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("monthKey")],
)
@Serializable
data class BudgetEntity(
    @PrimaryKey val id: String,
    val monthKey: String,
    val categoryId: String?,
    val amountMinor: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
