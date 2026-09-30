package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_goals WHERE isArchived = 0 ORDER BY createdAtEpochMillis")
    fun observeActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM goal_allocations WHERE goalId = :goalId ORDER BY allocatedAtEpochMillis")
    fun observeAllocations(goalId: String): Flow<List<GoalAllocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Insert
    suspend fun insertAllocation(allocation: GoalAllocationEntity)

    @Query("UPDATE savings_goals SET isArchived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveGoal(id: String, updatedAt: Long)
}
