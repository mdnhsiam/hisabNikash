package com.hisabnikash.app.feature.settings

import com.hisabnikash.app.data.repository.ClearableRepository
import com.hisabnikash.app.backup.BackupOperationGate

class DataDeletionUseCase(
    private val repositories: List<ClearableRepository>,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { operation -> operation() },
    private val operationGate: BackupOperationGate = BackupOperationGate(),
    private val clearPrivatePreferences: suspend () -> Unit,
) {
    suspend fun deleteAllLocalData(confirmed: Boolean): Boolean = operationGate.run {
        if (!confirmed) return@run false
        operationGate.invalidateAuthorization()
        // Disable backup and clear secrets before repository callbacks can enqueue work.
        clearPrivatePreferences()
        inTransaction {
            repositories.forEach { it.clearAll() }
        }
        true
    }
}
