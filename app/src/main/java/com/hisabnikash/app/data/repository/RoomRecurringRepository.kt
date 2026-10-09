package com.hisabnikash.app.data.repository

import androidx.room.withTransaction
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.money.FinancialLimits
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import com.hisabnikash.app.feature.recurring.RecurringDatePolicy
import com.hisabnikash.app.feature.recurring.RecurringEditor
import com.hisabnikash.app.feature.recurring.toTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecurringRepository(
    private val database: HisabDatabase,
    private val transactionRepository: TransactionRepository,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onDataChanged: () -> Unit = {},
) : RecurringRepository {
    private val dao = database.recurringDao()

    override fun observeAll(): Flow<List<RecurringItem>> = dao.observeAll().map { rows -> rows.map(RecurringItemEntity::toDomain) }
    override suspend fun find(id: String): RecurringItem? = dao.findById(id)?.toDomain()

    override suspend fun upsert(item: RecurringItem) {
        require(item.id.isNotBlank())
        FinancialLimits.requirePositive(item.amountMinor)
        require(item.description.isNotBlank())
        database.withTransaction {
            val expected = if (item.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            item.categoryId?.let { require(database.categoryDao().findById(it)?.type == expected) }
            dao.upsert(item.toEntity())
        }
        onDataChanged()
    }

    override suspend fun delete(id: String) { dao.deleteById(id); onDataChanged() }
    override suspend fun clearAll() { dao.deleteAll(); onDataChanged() }

    override suspend fun confirmOccurrence(id: String, expectedDueAtEpochMillis: Long, edits: RecurringEditor?): Transaction {
        val now = nowEpochMillis()
        return database.withTransaction {
            val item = requireNotNull(dao.findById(id)?.toDomain()) { "Recurring item not found" }
            require(item.isEnabled)
            require(item.nextDueAtEpochMillis == expectedDueAtEpochMillis) { "Recurring occurrence is stale" }
            val transaction = item.toTransaction(edits, now, newId())
            transactionRepository.add(transaction)
            dao.upsert(item.copy(nextDueAtEpochMillis = RecurringDatePolicy.nextDue(item.nextDueAtEpochMillis, now), updatedAtEpochMillis = now).toEntity())
            transaction
        }
            .also { onDataChanged() }
    }

    override suspend fun skipOccurrence(id: String, expectedDueAtEpochMillis: Long) {
        val now = nowEpochMillis()
        database.withTransaction {
            val item = requireNotNull(find(id))
            require(item.isEnabled && item.nextDueAtEpochMillis == expectedDueAtEpochMillis) { "Recurring occurrence is stale" }
            dao.upsert(item.copy(nextDueAtEpochMillis = RecurringDatePolicy.nextDue(item.nextDueAtEpochMillis, now), updatedAtEpochMillis = now).toEntity())
        }
        onDataChanged()
    }

    override suspend fun disable(id: String) {
        database.withTransaction {
            val item = requireNotNull(find(id))
            dao.upsert(item.copy(isEnabled = false, updatedAtEpochMillis = nowEpochMillis()).toEntity())
        }
        onDataChanged()
    }
}

private fun RecurringItem.toEntity() = RecurringItemEntity(id, type, amountMinor, description, categoryId, paymentMethod, note, frequency, reminderStyle, nextDueAtEpochMillis, isEnabled, createdAtEpochMillis, updatedAtEpochMillis)
private fun RecurringItemEntity.toDomain() = RecurringItem(id, type, amountMinor, description, categoryId, paymentMethod, note, frequency, reminderStyle, nextDueAtEpochMillis, isEnabled, createdAtEpochMillis, updatedAtEpochMillis)
