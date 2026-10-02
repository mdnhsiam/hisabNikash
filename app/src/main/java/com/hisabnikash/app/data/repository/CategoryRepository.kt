package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository : ClearableRepository {
    fun observeActive(type: CategoryType): Flow<List<Category>>

    suspend fun add(category: Category)

    suspend fun update(category: Category)

    suspend fun archive(id: String)

    override suspend fun clearAll() = Unit
}

object DefaultCategories {
    private fun category(
        id: String,
        type: CategoryType,
        nameEn: String,
        nameBn: String,
        order: Int,
    ) = Category(id, type, nameEn, nameBn, order, isSystem = true)

    val all: List<Category> = listOf(
        category("income_salary", CategoryType.INCOME, "Salary", "বেতন", 0),
        category("income_business", CategoryType.INCOME, "Business", "ব্যবসা", 1),
        category("income_gift", CategoryType.INCOME, "Gift", "উপহার", 2),
        category("income_other", CategoryType.INCOME, "Others", "অন্যান্য", 3),
        category("expense_food", CategoryType.EXPENSE, "Food", "খাবার", 0),
        category("expense_transport", CategoryType.EXPENSE, "Transport", "যাতায়াত", 1),
        category("expense_groceries", CategoryType.EXPENSE, "Groceries", "বাজার", 2),
        category("expense_bills", CategoryType.EXPENSE, "Bills", "বিল", 3),
        category("expense_medical", CategoryType.EXPENSE, "Medical", "চিকিৎসা", 4),
        category("expense_shopping", CategoryType.EXPENSE, "Shopping", "কেনাকাটা", 5),
        category("expense_education", CategoryType.EXPENSE, "Education", "শিক্ষা", 6),
        category("expense_entertainment", CategoryType.EXPENSE, "Entertainment", "বিনোদন", 7),
        category("expense_other", CategoryType.EXPENSE, "Others", "অন্যান্য", 8),
    )

    fun forType(type: CategoryType): List<Category> = all.filter { it.type == type }
}
