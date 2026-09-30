package com.hisabnikash.app.core.model

data class Category(
    val id: String,
    val type: CategoryType,
    val nameEn: String,
    val nameBn: String,
    val sortOrder: Int,
    val isSystem: Boolean,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long = 0L,
)
