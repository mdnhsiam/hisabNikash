package com.hisabnikash.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.core.model.Budget
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.RecurrenceFrequency
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.db.entity.BudgetEntity
import com.hisabnikash.app.data.db.entity.CategoryEntity
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import com.hisabnikash.app.data.db.entity.TransactionEntity
import com.hisabnikash.app.data.repository.DefaultCategories
import com.hisabnikash.app.domain.FinanceCalculator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReleaseFixtureRoundTripTest {
    private lateinit var database: HisabDatabase

    @Before fun setUp() { database = HisabDatabase.createInMemory(ApplicationProvider.getApplicationContext<Context>()) }
    @After fun tearDown() = database.close()

    @Test fun `complete finance fixture restores identical dashboard and reports`() = runTest {
        insertReleaseFixture(database)
        val exporter = SnapshotExporter(database, nowEpochMillis = { 1_750_000_000_000 })
        val importer = SnapshotImporter(database, BackupCrypto(10_000))
        val original = exporter.export()
        val month = MonthKey(2025, 2)
        val originalDashboard = FinanceCalculator.dashboard(
            original.transactions.map { it.toDomain() },
            original.budgets.map { it.toDomain() },
            original.goalAllocations.map { it.toDomain() },
            month,
        )
        val originalReport = FinanceCalculator.monthlyReport(original.transactions.map { it.toDomain() }, month)
        val encrypted = importer.encode(original, "release-pass".toCharArray())

        database.snapshotDao().replaceAll(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(RestoreResult.Success, importer.apply(importer.validate(encrypted, "release-pass".toCharArray())))
        val restored = exporter.export()

        assertEquals(original.copy(exportedAtEpochMillis = restored.exportedAtEpochMillis), restored)
        assertEquals(originalDashboard, FinanceCalculator.dashboard(restored.transactions.map { it.toDomain() }, restored.budgets.map { it.toDomain() }, restored.goalAllocations.map { it.toDomain() }, month))
        assertEquals(originalReport, FinanceCalculator.monthlyReport(restored.transactions.map { it.toDomain() }, month))
    }
}

private suspend fun insertReleaseFixture(database: HisabDatabase) {
    val customCategory = CategoryEntity("custom_tuition", CategoryType.EXPENSE, "Tuition", "টিউশন", 90, false, true, 1)
    fun transaction(id: String, type: TransactionType, amount: Long, categoryId: String?, payment: PaymentMethod, at: Long) =
        TransactionEntity(id, type, amount, id, id, categoryId, null, payment, null, at, at, at)
    database.snapshotDao().replaceAll(
        categories = DefaultCategories.all.map { category ->
            CategoryEntity(category.id, category.type, category.nameEn, category.nameBn, category.sortOrder, category.isSystem, category.isActive, category.createdAtEpochMillis)
        } + customCategory,
        transactions = listOf(
            transaction("jan-income", TransactionType.INCOME, 90_000, null, PaymentMethod.BANK, 1_736_899_200_000),
            transaction("jan-expense", TransactionType.EXPENSE, 20_000, "custom_tuition", PaymentMethod.BKASH, 1_737_158_400_000),
            transaction("feb-income", TransactionType.INCOME, 100_000, null, PaymentMethod.BANK, 1_738_454_400_000),
            transaction("feb-expense", TransactionType.EXPENSE, 25_000, "custom_tuition", PaymentMethod.CASH, 1_738_713_600_000),
        ),
        budgets = listOf(BudgetEntity("feb-total", "2025-02", null, 50_000, 2, 2)),
        savingsGoals = listOf(SavingsGoalEntity("emergency", "Emergency", 300_000, false, 3, 3)),
        goalAllocations = listOf(GoalAllocationEntity("allocation-1", "emergency", 10_000, null, 1_738_800_000_000, null)),
        recurringItems = listOf(
            RecurringItemEntity("rent", TransactionType.EXPENSE, 15_000, "Rent", "custom_tuition", PaymentMethod.BANK, null, RecurrenceFrequency.MONTHLY, ReminderStyle.OPEN_AND_REVIEW, 1_741_046_400_000, true, 4, 4),
        ),
    )
}

private fun TransactionEntity.toDomain() = Transaction(id, type, amountMinor, description, categoryId, oneOffCategoryLabel, paymentMethod, note, occurredAtEpochMillis, createdAtEpochMillis, updatedAtEpochMillis)
private fun BudgetEntity.toDomain() = Budget(id, monthKey, categoryId, amountMinor, createdAtEpochMillis, updatedAtEpochMillis)
private fun GoalAllocationEntity.toDomain() = GoalAllocation(id, goalId, amountMinor, note, allocatedAtEpochMillis, reversesAllocationId)
