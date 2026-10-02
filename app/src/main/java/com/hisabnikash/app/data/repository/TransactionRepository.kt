package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.Category
import kotlinx.coroutines.flow.Flow

interface TransactionRepository : ClearableRepository {
    fun observeAll(): Flow<List<Transaction>>

    suspend fun add(transaction: Transaction)

    suspend fun addWithCustomCategory(transaction: Transaction, category: Category)

    suspend fun update(transaction: Transaction)

    suspend fun delete(id: String)

    suspend fun findPossibleDuplicate(candidate: Transaction): Transaction?

    override suspend fun clearAll() = Unit
}

object TransactionDuplicatePolicy {
    const val WINDOW_MILLIS: Long = 60_000L

    fun isPossibleDuplicate(existing: Transaction, candidate: Transaction): Boolean =
        existing.id != candidate.id &&
            existing.type == candidate.type &&
            existing.amountMinor == candidate.amountMinor &&
            normalizeDescription(existing.description) == normalizeDescription(candidate.description) &&
            existing.categoryId == candidate.categoryId &&
            existing.paymentMethod == candidate.paymentMethod &&
            kotlin.math.abs(existing.createdAtEpochMillis - candidate.createdAtEpochMillis) <= WINDOW_MILLIS

    fun normalizeDescription(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
