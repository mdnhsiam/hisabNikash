package com.hisabnikash.app.backup.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.HisabApplication
import kotlinx.coroutines.flow.first

object DriveBackupWorkerPolicy {
    fun shouldRetry(error: Throwable): Boolean =
        error is DriveOfflineException || error is DriveRetryableException || error is DriveQuotaException
}

class DriveBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HisabApplication).container
        if (!container.userPreferences.state.first().driveBackupEnabled) return Result.success()
        val password = container.backupPasswordCache.load() ?: return Result.failure()
        val token = container.driveAuthorizationClient.currentAccessToken() ?: return Result.failure()
        return try {
            container.driveBackupStore.backUpNow(token, password)
            Result.success()
        } catch (error: Throwable) {
            if (DriveBackupWorkerPolicy.shouldRetry(error)) Result.retry() else Result.failure()
        } finally {
            password.fill('\u0000')
        }
    }
}
