package com.hisabnikash.app.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.hisabnikash.app.backup.local.LocalBackupWorker
import com.hisabnikash.app.backup.drive.DriveBackupWorker

interface BackupWorkGateway {
    fun enqueueUnique(name: String)
    fun enqueueUniqueDrive(name: String) = enqueueUnique(name)
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
    override fun enqueueUniqueDrive(name: String) {
        val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }
}

class BackupScheduler(private val gateway: BackupWorkGateway) {
    fun onDataChanged() {
        // Backup scheduling must never make a successful local ledger write fail.
        runCatching { gateway.enqueueUnique(UNIQUE_LOCAL_BACKUP) }
    }
    fun onCloudDataChanged() {
        runCatching { gateway.enqueueUniqueDrive(UNIQUE_DRIVE_BACKUP) }
    }

    companion object {
        const val UNIQUE_LOCAL_BACKUP = "local-encrypted-backup"
        const val UNIQUE_DRIVE_BACKUP = "drive-encrypted-backup"
    }
}
