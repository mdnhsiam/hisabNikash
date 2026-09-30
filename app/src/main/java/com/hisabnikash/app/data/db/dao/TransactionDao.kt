package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.db.entity.CategoryEntity
import com.hisabnikash.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC")
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM transactions
        WHERE id != :candidateId
          AND type = :type
          AND amountMinor = :amountMinor
          AND normalizedDescription = :normalizedDescription
          AND ((categoryId = :categoryId) OR (categoryId IS NULL AND :categoryId IS NULL))
          AND paymentMethod = :paymentMethod
          AND createdAtEpochMillis BETWEEN :windowStart AND :windowEnd
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun findPossibleDuplicate(
        candidateId: String,
        type: TransactionType,
        amountMinor: Long,
        normalizedDescription: String,
        categoryId: String?,
        paymentMethod: PaymentMethod,
        windowStart: Long,
        windowEnd: Long,
    ): TransactionEntity?
}
