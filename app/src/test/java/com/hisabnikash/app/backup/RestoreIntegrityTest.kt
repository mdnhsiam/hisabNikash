package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.core.model.*
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.data.db.entity.*
import com.hisabnikash.app.data.repository.DefaultCategories
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreIntegrityTest {
    private fun snapshot() = BackupSnapshotV1.empty(1).copy(categories = DefaultCategories.all.map {
        CategoryEntity(it.id, it.type, it.nameEn, it.nameBn, it.sortOrder, it.isSystem, it.isActive, it.createdAtEpochMillis)
    })
    private fun income(amount: Long = 100) = TransactionEntity("income", TransactionType.INCOME, amount,
        "Income", "income", "income_salary", null, PaymentMethod.BANK, null, 1, 1, 1)
    private fun allocation(id: String, amount: Long, reverses: String? = null) =
        GoalAllocationEntity(id, "goal", amount, null, 1, reverses)
    private fun withAllocations(vararg rows: GoalAllocationEntity) = snapshot().copy(
        savingsGoals = listOf(SavingsGoalEntity("goal", "Emergency", null, false, 1, 1)),
        goalAllocations = rows.toList(),
    )
    private fun rejected(value: BackupSnapshotV1) = assertTrue("Invalid snapshot must be rejected before any replacement",
        runCatching { RestoreValidator.validate(value) }.exceptionOrNull() is InvalidBackupException)

    @Test fun `import cannot bypass the supported money bound`() = rejected(snapshot().copy(
        transactions = listOf(income(MoneyParser.MAX_MINOR_UNITS + 1))))

    @Test fun `income cannot reference an expense category`() = rejected(snapshot().copy(
        transactions = listOf(income().copy(categoryId = "expense_food"))))

    @Test fun `two reversal rows cannot silently replace each other`() = rejected(withAllocations(
        allocation("original", 100), allocation("reverse-one", -100, "original"), allocation("reverse-two", -100, "original")))

    @Test fun `a negative contribution without an original is rejected`() = rejected(withAllocations(allocation("negative", -100)))

    @Test fun `a reversal must match its original amount and goal`() = rejected(withAllocations(
        allocation("original", 100), allocation("reverse", -200, "original")))

    @Test fun `a reversal must reference an existing positive contribution`() = rejected(withAllocations(
        allocation("reverse", -100, "missing")))

    @Test fun `budget month must be a canonical month key`() = rejected(snapshot().copy(
        budgets = listOf(BudgetEntity("budget", "2025-99", null, 100, 1, 1))))

    @Test fun `duplicate budget slots are rejected instead of choosing an arbitrary limit`() = rejected(snapshot().copy(
        budgets = listOf(BudgetEntity("a", "2025-02", null, 100, 1, 1), BudgetEntity("b", "2025-02", null, 200, 1, 1))))

    @Test fun `valid reversible history remains compatible`() {
        RestoreValidator.validate(withAllocations(allocation("original", 100), allocation("reverse", -100, "original")))
    }
}
