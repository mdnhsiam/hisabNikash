package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_goals WHERE isArchived = 0 ORDER BY createdAtEpochMillis")
    fun observeActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM goal_allocations WHERE goalId = :goalId ORDER BY allocatedAtEpochMillis")
    fun observeAllocations(goalId: String): Flow<List<GoalAllocationEntity>>

    @Query("SELECT * FROM goal_allocations ORDER BY allocatedAtEpochMillis")
    fun observeAllAllocations(): Flow<List<GoalAllocationEntity>>

    @Query("SELECT * FROM goal_allocations WHERE id = :id")
    suspend fun findAllocation(id: String): GoalAllocationEntity?

    @Query("SELECT COUNT(*) FROM goal_allocations WHERE reversesAllocationId = :allocationId")
    suspend fun reversalCount(allocationId: String): Int

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun findGoal(id: String): SavingsGoalEntity?

    @Upsert
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Insert
    suspend fun insertAllocation(allocation: GoalAllocationEntity)

    @Query("UPDATE savings_goals SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveGoal(id: String, updatedAt: Long)

    @Query("DELETE FROM goal_allocations")
    suspend fun deleteAllAllocations()

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllGoals()
}
