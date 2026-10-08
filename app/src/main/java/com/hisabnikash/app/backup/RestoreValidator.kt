package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.data.repository.DefaultCategories
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.money.FinancialLimits
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.core.time.MonthKey

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UnsupportedBackupVersionException(val version: Int) : Exception("Unsupported backup version")

object RestoreValidator {
    fun validate(snapshot: BackupSnapshotV1): ValidatedRestore {
        if (snapshot.schemaVersion != BackupSnapshotV1.SCHEMA_VERSION) {
            throw UnsupportedBackupVersionException(snapshot.schemaVersion)
        }
        requireUnique(snapshot.transactions.map { it.id }, "transactions")
        requireUnique(snapshot.categories.map { it.id }, "categories")
        requireUnique(snapshot.budgets.map { it.id }, "budgets")
        requireUnique(snapshot.savingsGoals.map { it.id }, "savings goals")
        requireUnique(snapshot.goalAllocations.map { it.id }, "goal allocations")
        requireUnique(snapshot.recurringItems.map { it.id }, "recurring items")

        val categories = snapshot.categories.associateBy { it.id }
        val categoryIds = categories.keys
        val goalIds = snapshot.savingsGoals.mapTo(hashSetOf()) { it.id }
        if (!categoryIds.containsAll(DefaultCategories.all.map { it.id })) {
            throw InvalidBackupException("Required categories are missing")
        }
        fun validAmount(amount: Long) = amount in 1..MoneyParser.MAX_MINOR_UNITS
        fun validCategory(id: String?, type: TransactionType) = id == null || categories[id]?.type ==
            if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        if (snapshot.categories.any { it.nameEn.isBlank() || it.nameBn.isBlank() } ||
            DefaultCategories.all.any { categories[it.id]?.type != it.type || categories[it.id]?.isSystem != true }) {
            throw InvalidBackupException("Invalid category data")
        }
        if (snapshot.transactions.any { !validAmount(it.amountMinor) || it.description.isBlank() || !validCategory(it.categoryId, it.type) }) {
            throw InvalidBackupException("Invalid transaction data")
        }
        if (runCatching { FinancialLimits.total(snapshot.transactions.map { it.amountMinor }) }
                .getOrDefault(Long.MAX_VALUE) > FinancialLimits.MAX_LEDGER_TOTAL_MINOR) {
            throw InvalidBackupException("Ledger total exceeds the supported range")
        }
        if (snapshot.budgets.any { !validAmount(it.amountMinor) ||
                !validCategory(it.categoryId, TransactionType.EXPENSE) ||
                runCatching { MonthKey.parse(it.monthKey).value }.getOrNull() != it.monthKey } ||
            snapshot.budgets.map { it.monthKey to it.categoryId }.distinct().size != snapshot.budgets.size) {
            throw InvalidBackupException("Invalid budget data")
        }
        if (snapshot.savingsGoals.any { it.name.isBlank() || it.targetMinor != null && !validAmount(it.targetMinor) }) {
            throw InvalidBackupException("Invalid savings goal data")
        }
        val allocations = snapshot.goalAllocations.associateBy { it.id }
        val reversed = hashSetOf<String>()
        if (snapshot.goalAllocations.any { row ->
            if (row.goalId !in goalIds) true
            else if (row.reversesAllocationId == null) !validAmount(row.amountMinor)
            else {
                val original = allocations[row.reversesAllocationId]
                original == null || original.reversesAllocationId != null || !validAmount(original.amountMinor) ||
                    row.goalId != original.goalId || row.amountMinor != -original.amountMinor || !reversed.add(original.id)
            }
        } || runCatching { FinancialLimits.total(snapshot.goalAllocations.filter { it.amountMinor > 0 }.map { it.amountMinor }) }
                .getOrDefault(Long.MAX_VALUE) > FinancialLimits.MAX_LEDGER_TOTAL_MINOR) {
            throw InvalidBackupException("Invalid allocation data")
        }
        if (snapshot.recurringItems.any { !validAmount(it.amountMinor) || it.description.isBlank() || !validCategory(it.categoryId, it.type) }) {
            throw InvalidBackupException("Invalid recurring data")
        }
        return ValidatedRestore(snapshot)
    }

    private fun requireUnique(ids: List<String>, table: String) {
        if (ids.any(String::isBlank) || ids.size != ids.toSet().size) throw InvalidBackupException("Invalid $table identifiers")
    }
}

class ValidatedRestore internal constructor(internal val snapshot: BackupSnapshotV1)
