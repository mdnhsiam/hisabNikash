package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.time.MonthKey
import kotlinx.coroutines.flow.Flow

interface BudgetRepository : ClearableRepository {
    fun observeMonth(month: MonthKey): Flow<List<Budget>>
    suspend fun upsert(budget: Budget)
    suspend fun delete(id: String)
    override suspend fun clearAll() = Unit
}
