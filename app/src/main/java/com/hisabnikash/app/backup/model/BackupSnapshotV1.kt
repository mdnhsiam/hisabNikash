package com.hisabnikash.app.backup.model

import com.hisabnikash.app.data.db.entity.BudgetEntity
import com.hisabnikash.app.data.db.entity.CategoryEntity
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import com.hisabnikash.app.data.db.entity.TransactionEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshotV1(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val goalAllocations: List<GoalAllocationEntity>,
    val recurringItems: List<RecurringItemEntity>,
) {
    companion object {
        const val SCHEMA_VERSION = 1

        fun empty(exportedAtEpochMillis: Long) = BackupSnapshotV1(
            schemaVersion = SCHEMA_VERSION,
            exportedAtEpochMillis = exportedAtEpochMillis,
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            savingsGoals = emptyList(),
            goalAllocations = emptyList(),
            recurringItems = emptyList(),
        )
    }
}

object BackupWireCodec {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encodeEnvelope(envelope: BackupEnvelope): ByteArray = json.encodeToString(envelope).encodeToByteArray()
    fun decodeEnvelope(bytes: ByteArray): BackupEnvelope = json.decodeFromString(bytes.decodeToString())
    fun encodeSnapshot(snapshot: BackupSnapshotV1): ByteArray = json.encodeToString(snapshot).encodeToByteArray()
    fun decodeSnapshot(bytes: ByteArray): BackupSnapshotV1 = json.decodeFromString(bytes.decodeToString())
}
