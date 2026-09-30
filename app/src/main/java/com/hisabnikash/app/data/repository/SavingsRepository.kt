package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    fun observeGoals(): Flow<List<SavingsGoal>>
    fun observeAllocations(): Flow<List<GoalAllocation>>
    suspend fun upsertGoal(goal: SavingsGoal)
    suspend fun archiveGoal(id: String, updatedAtEpochMillis: Long)
    suspend fun allocate(allocation: GoalAllocation, availableSavingsMinor: Long)
    suspend fun reverse(allocationId: String, reversalId: String, atEpochMillis: Long)
}
