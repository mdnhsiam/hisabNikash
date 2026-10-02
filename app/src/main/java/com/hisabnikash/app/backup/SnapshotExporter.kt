package com.hisabnikash.app.backup

import androidx.room.withTransaction
import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.data.db.HisabDatabase

class SnapshotExporter(
    private val database: HisabDatabase,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun export(): BackupSnapshotV1 = database.withTransaction {
        val dao = database.snapshotDao()
        BackupSnapshotV1(
            schemaVersion = BackupSnapshotV1.SCHEMA_VERSION,
            exportedAtEpochMillis = nowEpochMillis(),
            transactions = dao.transactions(),
            categories = dao.categories(),
            budgets = dao.budgets(),
            savingsGoals = dao.savingsGoals(),
            goalAllocations = dao.goalAllocations(),
            recurringItems = dao.recurringItems(),
        )
    }
}
