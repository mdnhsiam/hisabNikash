package com.hisabnikash.app.data.repository

import androidx.room.withTransaction
import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomBudgetRepository(
    private val database: HisabDatabase,
) : BudgetRepository {
    private val dao = database.budgetDao()

    override fun observeMonth(month: MonthKey): Flow<List<Budget>> =
        dao.observeMonth(month.value).map { rows -> rows.map(BudgetEntity::toDomain) }

    override suspend fun upsert(budget: Budget) {
        require(budget.amountMinor > 0)
        database.withTransaction {
            dao.find(budget.monthKey, budget.categoryId)?.takeIf { it.id != budget.id }?.let { dao.deleteById(it.id) }
            dao.upsert(budget.toEntity())
        }
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}

private fun Budget.toEntity() = BudgetEntity(id, monthKey, categoryId, amountMinor, createdAtEpochMillis, updatedAtEpochMillis)
private fun BudgetEntity.toDomain() = Budget(id, monthKey, categoryId, amountMinor, createdAtEpochMillis, updatedAtEpochMillis)
