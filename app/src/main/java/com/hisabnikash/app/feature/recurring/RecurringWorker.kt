package com.hisabnikash.app.feature.recurring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabnikash.app.HisabApplication

enum class RecurringWorkerDecision { POST_REMINDER, NO_OP }

object RecurringWorkerPolicy {
    fun decide(permissionGranted: Boolean, enabled: Boolean, isDue: Boolean): RecurringWorkerDecision =
        if (NotificationPolicy.canPost(permissionGranted, enabled, isDue)) RecurringWorkerDecision.POST_REMINDER else RecurringWorkerDecision.NO_OP
}

class RecurringWorker(
    appContext: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val recurringId = inputData.getString(KEY_RECURRING_ID) ?: return Result.failure()
        val container = (applicationContext as HisabApplication).container
        return container.backupOperationGate.run {
            val item = container.recurringRepository.find(recurringId) ?: return@run Result.success()
            val permissionGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            when (RecurringWorkerPolicy.decide(permissionGranted, item.isEnabled, item.nextDueAtEpochMillis <= System.currentTimeMillis())) {
                RecurringWorkerDecision.POST_REMINDER -> {
                    NotificationFactory(applicationContext).show(item)
                    Result.success()
                }
                RecurringWorkerDecision.NO_OP -> Result.success()
            }
        }
    }

    companion object {
        const val KEY_RECURRING_ID = "recurring_id"
    }
}
