package com.hisabnikash.app.data.repository

import androidx.room.withTransaction
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.SavingsGoal
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSavingsRepository(
    private val database: HisabDatabase,
) : SavingsRepository {
    private val dao = database.savingsDao()

    override fun observeGoals(): Flow<List<SavingsGoal>> = dao.observeActiveGoals().map { rows -> rows.map(SavingsGoalEntity::toDomain) }
    override fun observeAllocations(): Flow<List<GoalAllocation>> = dao.observeAllAllocations().map { rows -> rows.map(GoalAllocationEntity::toDomain) }

    override suspend fun upsertGoal(goal: SavingsGoal) {
        require(goal.name.isNotBlank())
        require(goal.targetMinor == null || goal.targetMinor > 0)
        dao.upsertGoal(goal.toEntity())
    }

    override suspend fun archiveGoal(id: String, updatedAtEpochMillis: Long) = dao.archiveGoal(id, updatedAtEpochMillis)

    override suspend fun allocate(allocation: GoalAllocation, availableSavingsMinor: Long) {
        require(allocation.amountMinor > 0) { "Allocation must be positive" }
        require(allocation.amountMinor <= availableSavingsMinor.coerceAtLeast(0)) { "Allocation exceeds available savings" }
        database.withTransaction {
            val goal = requireNotNull(dao.findGoal(allocation.goalId)) { "Goal not found" }
            require(!goal.isArchived) { "Goal is archived" }
            dao.insertAllocation(allocation.toEntity())
        }
    }

    override suspend fun reverse(allocationId: String, reversalId: String, atEpochMillis: Long) {
        database.withTransaction {
            val original = requireNotNull(dao.findAllocation(allocationId)) { "Allocation not found" }
            require(original.amountMinor > 0) { "Only a contribution can be reversed" }
            require(dao.reversalCount(allocationId) == 0) { "Allocation is already reversed" }
            dao.insertAllocation(
                original.copy(
                    id = reversalId,
                    amountMinor = -original.amountMinor,
                    note = "Reversal",
                    allocatedAtEpochMillis = atEpochMillis,
                    reversesAllocationId = allocationId,
                ),
            )
        }
    }
}

private fun SavingsGoal.toEntity() = SavingsGoalEntity(id, name, targetMinor, isArchived, createdAtEpochMillis, updatedAtEpochMillis)
private fun SavingsGoalEntity.toDomain() = SavingsGoal(id, name, targetMinor, isArchived, createdAtEpochMillis, updatedAtEpochMillis)
private fun GoalAllocation.toEntity() = GoalAllocationEntity(id, goalId, amountMinor, note, allocatedAtEpochMillis, reversesAllocationId)
private fun GoalAllocationEntity.toDomain() = GoalAllocation(id, goalId, amountMinor, note, allocatedAtEpochMillis, reversesAllocationId)
