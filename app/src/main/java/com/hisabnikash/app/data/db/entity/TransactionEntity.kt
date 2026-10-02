package com.hisabnikash.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import kotlinx.serialization.Serializable

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("occurredAtEpochMillis"),
        Index(value = ["type", "amountMinor", "normalizedDescription", "paymentMethod", "createdAtEpochMillis"]),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val normalizedDescription: String,
    val categoryId: String?,
    val oneOffCategoryLabel: String?,
    val paymentMethod: PaymentMethod,
    val note: String?,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
