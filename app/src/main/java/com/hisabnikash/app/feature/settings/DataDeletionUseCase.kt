package com.hisabnikash.app.feature.settings

import com.hisabnikash.app.data.repository.ClearableRepository

class DataDeletionUseCase(
    private val repositories: List<ClearableRepository>,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { operation -> operation() },
    private val clearPrivatePreferences: suspend () -> Unit,
) {
    suspend fun deleteAllLocalData(confirmed: Boolean): Boolean {
        if (!confirmed) return false
        inTransaction {
            repositories.forEach { it.clearAll() }
        }
        clearPrivatePreferences()
        return true
    }
}
