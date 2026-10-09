package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.feature.recurring.RecurringEditor
import kotlinx.coroutines.flow.Flow

interface RecurringRepository : ClearableRepository {
    fun observeAll(): Flow<List<RecurringItem>>
    suspend fun find(id: String): RecurringItem?
    suspend fun upsert(item: RecurringItem)
    suspend fun delete(id: String)
    suspend fun confirmOccurrence(id: String, expectedDueAtEpochMillis: Long, edits: RecurringEditor? = null): Transaction
    suspend fun skipOccurrence(id: String, expectedDueAtEpochMillis: Long)
    suspend fun disable(id: String)
    override suspend fun clearAll() = Unit
}
