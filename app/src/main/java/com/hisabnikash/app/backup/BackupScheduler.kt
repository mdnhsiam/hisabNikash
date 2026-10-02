package com.hisabnikash.app.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hisabnikash.app.backup.local.LocalBackupWorker

interface BackupWorkGateway {
    fun enqueueUnique(name: String)
}

class WorkManagerBackupGateway(context: Context) : BackupWorkGateway {
    private val applicationContext = context.applicationContext
    override fun enqueueUnique(name: String) {
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<LocalBackupWorker>().build(),
        )
    }
}

class BackupScheduler(private val gateway: BackupWorkGateway) {
    fun onDataChanged() {
        // Backup scheduling must never make a successful local ledger write fail.
        runCatching { gateway.enqueueUnique(UNIQUE_LOCAL_BACKUP) }
    }

    companion object {
        const val UNIQUE_LOCAL_BACKUP = "local-encrypted-backup"
    }
}
