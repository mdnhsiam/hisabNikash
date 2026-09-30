package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRepositoryTest {
    @Test
    fun `default expense categories are practical and include Others`() {
        val labels = DefaultCategories.forType(CategoryType.EXPENSE).map { it.nameEn }

        assertEquals(
            listOf("Food", "Transport", "Groceries", "Bills", "Medical", "Shopping", "Education", "Entertainment", "Others"),
            labels,
        )
        assertTrue(DefaultCategories.forType(CategoryType.EXPENSE).all { it.isSystem })
    }

    @Test
    fun `default income categories use stable unique ids`() {
        val income = DefaultCategories.forType(CategoryType.INCOME)

        assertTrue(income.isNotEmpty())
        assertEquals(income.size, income.map { it.id }.toSet().size)
        assertTrue(income.all { it.type == CategoryType.INCOME && it.isActive })
    }
}
