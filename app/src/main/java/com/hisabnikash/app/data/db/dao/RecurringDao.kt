package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_items ORDER BY nextDueAtEpochMillis")
    fun observeAll(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    suspend fun findById(id: String): RecurringItemEntity?

    @Query("SELECT * FROM recurring_items WHERE isEnabled = 1 AND nextDueAtEpochMillis <= :nowEpochMillis")
    suspend fun findDue(nowEpochMillis: Long): List<RecurringItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecurringItemEntity)

    @Delete
    suspend fun delete(item: RecurringItemEntity)

    @Query("DELETE FROM recurring_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_items")
    suspend fun deleteAll()
}
