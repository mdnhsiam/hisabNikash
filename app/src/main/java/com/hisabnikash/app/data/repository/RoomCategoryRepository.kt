package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.data.db.dao.CategoryDao
import com.hisabnikash.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
    private val onDataChanged: () -> Unit = {},
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = categoryDao.observeAll().map { rows -> rows.map(CategoryEntity::toDomain) }
    override fun observeActive(type: CategoryType): Flow<List<Category>> =
        categoryDao.observeActive(type).map { rows -> rows.map(CategoryEntity::toDomain) }

    override suspend fun add(category: Category) {
        require(!category.isSystem) { "Custom categories cannot be marked as system categories" }
        categoryDao.insert(category.toEntity())
        onDataChanged()
    }

    override suspend fun update(category: Category) {
        categoryDao.update(category.toEntity())
        onDataChanged()
    }

    override suspend fun archive(id: String) {
        categoryDao.archive(id)
        onDataChanged()
    }

    override suspend fun clearAll() {
        categoryDao.deleteCustomCategories()
        categoryDao.reactivateSystemCategories()
        onDataChanged()
    }
}

internal fun Category.toEntity() = CategoryEntity(
    id = id,
    type = type,
    nameEn = nameEn,
    nameBn = nameBn,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
)

internal fun CategoryEntity.toDomain() = Category(
    id = id,
    type = type,
    nameEn = nameEn,
    nameBn = nameBn,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isActive = isActive,
    createdAtEpochMillis = createdAtEpochMillis,
)
