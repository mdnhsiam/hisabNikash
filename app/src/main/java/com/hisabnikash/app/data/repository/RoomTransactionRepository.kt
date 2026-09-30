package com.hisabnikash.app.data.repository

import androidx.room.withTransaction
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository(
    private val database: HisabDatabase,
) : TransactionRepository {
    private val transactionDao = database.transactionDao()

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toDomain) }

    override suspend fun add(transaction: Transaction) {
        validate(transaction)
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun addWithCustomCategory(transaction: Transaction, category: Category) {
        validate(transaction)
        require(transaction.categoryId == category.id)
        database.withTransaction {
            database.categoryDao().insert(category.toEntity())
            transactionDao.insert(transaction.toEntity())
        }
    }

    override suspend fun update(transaction: Transaction) {
        validate(transaction)
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun delete(id: String) {
        transactionDao.deleteById(id)
    }

    override suspend fun findPossibleDuplicate(candidate: Transaction): Transaction? =
        transactionDao.findPossibleDuplicate(
            candidateId = candidate.id,
            type = candidate.type,
            amountMinor = candidate.amountMinor,
            normalizedDescription = TransactionDuplicatePolicy.normalizeDescription(candidate.description),
            categoryId = candidate.categoryId,
            paymentMethod = candidate.paymentMethod,
            windowStart = candidate.createdAtEpochMillis - TransactionDuplicatePolicy.WINDOW_MILLIS,
            windowEnd = candidate.createdAtEpochMillis + TransactionDuplicatePolicy.WINDOW_MILLIS,
        )?.toDomain()

    private fun validate(transaction: Transaction) {
        require(transaction.amountMinor > 0) { "Amount must be positive" }
        require(transaction.description.isNotBlank()) { "Description is required" }
    }
}

internal fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type,
    amountMinor = amountMinor,
    description = description.trim(),
    normalizedDescription = TransactionDuplicatePolicy.normalizeDescription(description),
    categoryId = categoryId,
    oneOffCategoryLabel = oneOffCategoryLabel?.trim()?.ifEmpty { null },
    paymentMethod = paymentMethod,
    note = note?.trim()?.ifEmpty { null },
    occurredAtEpochMillis = occurredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = type,
    amountMinor = amountMinor,
    description = description,
    categoryId = categoryId,
    oneOffCategoryLabel = oneOffCategoryLabel,
    paymentMethod = paymentMethod,
    note = note,
    occurredAtEpochMillis = occurredAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
