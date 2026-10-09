package com.hisabnikash.app.feature.recurring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hisabnikash.app.HisabApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationFactory.EXTRA_RECURRING_ID) ?: return
        val expectedDueAt = intent.getLongExtra(NotificationFactory.EXTRA_DUE_AT, Long.MIN_VALUE)
        if (intent.action !in setOf(ACTION_CONFIRM, ACTION_SKIP, ACTION_DISABLE)) return
        val pending = goAsync()
        val container = (context.applicationContext as HisabApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_CONFIRM -> container.recurringRepository.confirmOccurrence(id, expectedDueAt)
                    ACTION_SKIP -> container.recurringRepository.skipOccurrence(id, expectedDueAt)
                    ACTION_DISABLE -> container.recurringRepository.disable(id)
                }
                val updated = container.recurringRepository.find(id)
                if (updated?.isEnabled == true) container.recurringScheduler.schedule(updated) else container.recurringScheduler.cancel(id)
                androidx.core.app.NotificationManagerCompat.from(context).cancel(id, 0)
            } catch (_: Exception) {
                // Stale actions/storage failures leave the ledger unchanged and can be reviewed in-app.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.hisabnikash.app.recurring.CONFIRM"
        const val ACTION_SKIP = "com.hisabnikash.app.recurring.SKIP"
        const val ACTION_DISABLE = "com.hisabnikash.app.recurring.DISABLE"
    }
}

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val container = (context.applicationContext as HisabApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.recurringRepository.observeAll().first().forEach(container.recurringScheduler::schedule)
            } catch (_: Exception) {
                // WorkManager also persists reminders; a failed boot reschedule must not crash the process.
            } finally {
                pending.finish()
            }
        }
    }
}
