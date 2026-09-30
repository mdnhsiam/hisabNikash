package com.hisabnikash.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hisabnikash.app.core.model.CategoryType

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "isActive", "sortOrder"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val type: CategoryType,
    val nameEn: String,
    val nameBn: String,
    val sortOrder: Int,
    val isSystem: Boolean,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
)
