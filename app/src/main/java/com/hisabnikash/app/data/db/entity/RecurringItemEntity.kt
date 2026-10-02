package com.hisabnikash.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.RecurrenceFrequency
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.model.TransactionType
import kotlinx.serialization.Serializable

@Entity(
    tableName = "recurring_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index(value = ["isEnabled", "nextDueAtEpochMillis"])],
)
@Serializable
data class RecurringItemEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val note: String?,
    val frequency: RecurrenceFrequency,
    val reminderStyle: ReminderStyle,
    val nextDueAtEpochMillis: Long,
    val isEnabled: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
