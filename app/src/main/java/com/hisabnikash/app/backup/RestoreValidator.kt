package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.data.repository.DefaultCategories

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

        val categoryIds = snapshot.categories.mapTo(hashSetOf()) { it.id }
        val goalIds = snapshot.savingsGoals.mapTo(hashSetOf()) { it.id }
        if (!categoryIds.containsAll(DefaultCategories.all.map { it.id })) {
            throw InvalidBackupException("Required categories are missing")
        }
        if (snapshot.transactions.any { it.amountMinor <= 0 || it.description.isBlank() || it.categoryId != null && it.categoryId !in categoryIds }) {
            throw InvalidBackupException("Invalid transaction data")
        }
        if (snapshot.budgets.any { it.amountMinor <= 0 || it.categoryId != null && it.categoryId !in categoryIds }) {
            throw InvalidBackupException("Invalid budget data")
        }
        if (snapshot.savingsGoals.any { it.name.isBlank() || it.targetMinor != null && it.targetMinor <= 0 }) {
            throw InvalidBackupException("Invalid savings goal data")
        }
        if (snapshot.goalAllocations.any { it.goalId !in goalIds }) {
            throw InvalidBackupException("Invalid allocation data")
        }
        if (snapshot.recurringItems.any { it.amountMinor <= 0 || it.description.isBlank() || it.categoryId != null && it.categoryId !in categoryIds }) {
            throw InvalidBackupException("Invalid recurring data")
        }
        return ValidatedRestore(snapshot)
    }

    private fun requireUnique(ids: List<String>, table: String) {
        if (ids.any(String::isBlank) || ids.size != ids.toSet().size) throw InvalidBackupException("Invalid $table identifiers")
    }
}

class ValidatedRestore internal constructor(internal val snapshot: BackupSnapshotV1)
