package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hisabnikash.app.data.db.entity.BudgetEntity
import com.hisabnikash.app.data.db.entity.CategoryEntity
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import com.hisabnikash.app.data.db.entity.TransactionEntity

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<BudgetEntity>
    @Query("SELECT * FROM savings_goals") suspend fun savingsGoals(): List<SavingsGoalEntity>
    @Query("SELECT * FROM goal_allocations") suspend fun goalAllocations(): List<GoalAllocationEntity>
    @Query("SELECT * FROM recurring_items") suspend fun recurringItems(): List<RecurringItemEntity>

    @Query("DELETE FROM goal_allocations") suspend fun deleteGoalAllocations()
    @Query("DELETE FROM transactions") suspend fun deleteTransactions()
    @Query("DELETE FROM budgets") suspend fun deleteBudgets()
    @Query("DELETE FROM recurring_items") suspend fun deleteRecurringItems()
    @Query("DELETE FROM savings_goals") suspend fun deleteSavingsGoals()
    @Query("DELETE FROM categories") suspend fun deleteCategories()

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCategories(rows: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransactions(rows: List<TransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertBudgets(rows: List<BudgetEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSavingsGoals(rows: List<SavingsGoalEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGoalAllocations(rows: List<GoalAllocationEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRecurringItems(rows: List<RecurringItemEntity>)

    suspend fun replaceAll(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        goalAllocations: List<GoalAllocationEntity>,
        recurringItems: List<RecurringItemEntity>,
    ) {
        deleteGoalAllocations()
        deleteTransactions()
        deleteBudgets()
        deleteRecurringItems()
        deleteSavingsGoals()
        deleteCategories()
        insertCategories(categories)
        insertSavingsGoals(savingsGoals)
        insertTransactions(transactions)
        insertBudgets(budgets)
        insertRecurringItems(recurringItems)
        insertGoalAllocations(goalAllocations)
    }
}
