package com.hisabnikash.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, sortOrder, nameEn")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY sortOrder, nameEn")
    fun observeActive(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET isActive = 0 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("DELETE FROM categories WHERE isSystem = 0")
    suspend fun deleteCustomCategories()

    @Query("UPDATE categories SET isActive = 1 WHERE isSystem = 1")
    suspend fun reactivateSystemCategories()
}
