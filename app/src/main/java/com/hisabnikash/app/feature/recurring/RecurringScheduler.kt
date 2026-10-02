package com.hisabnikash.app.feature.recurring

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.time.MonthKey
import java.time.Instant
import java.util.concurrent.TimeUnit

data class ScheduledRecurringWork(
    val recurringId: String,
    val uniqueName: String,
    val delayMillis: Long,
)

interface RecurringWorkGateway {
    fun enqueueUnique(work: ScheduledRecurringWork)
    fun cancel(uniqueName: String)
}

class WorkManagerRecurringGateway(context: Context) : RecurringWorkGateway {
    private val workManager = WorkManager.getInstance(context)

    override fun enqueueUnique(work: ScheduledRecurringWork) {
        val request = OneTimeWorkRequestBuilder<RecurringWorker>()
            .setInitialDelay(work.delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(RecurringWorker.KEY_RECURRING_ID, work.recurringId).build())
            .addTag(work.uniqueName)
            .build()
        workManager.enqueueUniqueWork(work.uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(uniqueName: String) {
        workManager.cancelUniqueWork(uniqueName)
    }
}

class RecurringScheduler(
    private val gateway: RecurringWorkGateway,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    fun schedule(item: RecurringItem) {
        if (!item.isEnabled) return cancel(item.id)
        gateway.enqueueUnique(
            ScheduledRecurringWork(
                recurringId = item.id,
                uniqueName = uniqueName(item.id),
                delayMillis = (item.nextDueAtEpochMillis - nowEpochMillis()).coerceAtLeast(0),
            ),
        )
    }

    fun cancel(id: String) = gateway.cancel(uniqueName(id))

    fun rescheduleAll(items: List<RecurringItem>) {
        items.forEach { if (it.isEnabled) schedule(it) else cancel(it.id) }
    }

    companion object {
        fun uniqueName(id: String) = "recurring-$id"
    }
}

object RecurringDatePolicy {
    fun nextDue(currentDueEpochMillis: Long, afterEpochMillis: Long): Long {
        var due = Instant.ofEpochMilli(currentDueEpochMillis).atZone(MonthKey.DHAKA)
        do {
            due = due.plusMonths(1)
        } while (due.toInstant().toEpochMilli() <= afterEpochMillis)
        return due.toInstant().toEpochMilli()
    }
}

object NotificationPolicy {
    fun canPost(permissionGranted: Boolean, itemEnabled: Boolean, due: Boolean): Boolean = permissionGranted && itemEnabled && due
}
