package com.hisabnikash.app.backup.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.HisabApplication

object DriveBackupWorkerPolicy {
    fun shouldRetry(error: Throwable): Boolean =
        error is DriveOfflineException || error is DriveRetryableException || error is DriveQuotaException
}

class DriveBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HisabApplication).container
        return try {
            container.driveBackupStore.backUpQueued { container.driveAuthorizationClient.currentAccessToken() }
            Result.success()
        } catch (error: Throwable) {
            if (DriveBackupWorkerPolicy.shouldRetry(error)) Result.retry() else Result.failure()
        }
    }
}
