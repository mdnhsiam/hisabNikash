package com.hisabnikash.app.backup.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.HisabApplication

object LocalBackupWorkerPolicy {
    fun shouldRetry(error: Throwable): Boolean = error is BackupLocationUnavailableException
}

class LocalBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HisabApplication).container
        val password = container.backupPasswordCache.load() ?: return Result.success()
        return try {
            container.localBackupStore.backUpNow(password)
            Result.success()
        } catch (error: BackupNotEnabledException) {
            Result.success()
        } catch (error: Throwable) {
            if (LocalBackupWorkerPolicy.shouldRetry(error)) Result.retry() else Result.failure()
        } finally {
            password.fill('\u0000')
        }
    }
}
