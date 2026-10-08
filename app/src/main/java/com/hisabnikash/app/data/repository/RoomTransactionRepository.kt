package com.hisabnikash.app.data.repository

import androidx.room.withTransaction
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.TransactionEntity
import com.hisabnikash.app.core.money.FinancialLimits
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository(
    private val database: HisabDatabase,
    private val onDataChanged: () -> Unit = {},
) : TransactionRepository {
    private val transactionDao = database.transactionDao()

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toDomain) }

    override suspend fun add(transaction: Transaction) {
        database.withTransaction {
            validate(transaction)
            transactionDao.insert(transaction.toEntity())
        }
        onDataChanged()
    }

    override suspend fun addWithCustomCategory(transaction: Transaction, category: Category) {
        require(transaction.categoryId == category.id)
        database.withTransaction {
            validate(transaction, category)
            database.categoryDao().insert(category.toEntity())
            transactionDao.insert(transaction.toEntity())
        }
        onDataChanged()
    }

    override suspend fun update(transaction: Transaction) {
        database.withTransaction {
            validate(transaction)
            require(transactionDao.update(transaction.toEntity()) == 1) { "Transaction no longer exists" }
        }
        onDataChanged()
    }

    override suspend fun delete(id: String) {
        transactionDao.deleteById(id)
        onDataChanged()
    }

    override suspend fun clearAll() { transactionDao.deleteAll(); onDataChanged() }

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

    private suspend fun validate(transaction: Transaction, newCategory: Category? = null) {
        require(transaction.id.isNotBlank())
        FinancialLimits.requirePositive(transaction.amountMinor)
        require(transaction.description.isNotBlank()) { "Description is required" }
        transaction.categoryId?.let { id ->
            val type = newCategory?.takeIf { it.id == id }?.type ?: database.categoryDao().findById(id)?.type
            val expected = if (transaction.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            require(type == expected) { "Category does not match transaction type" }
        }
        require(transactionDao.totalAmountExcluding(transaction.id) <= FinancialLimits.MAX_LEDGER_TOTAL_MINOR - transaction.amountMinor) {
            "Ledger total exceeds the supported range"
        }
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
