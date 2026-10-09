package com.hisabnikash.app.feature.recurring

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hisabnikash.app.MainActivity
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.money.MoneyFormatter
import android.net.Uri

class NotificationFactory(private val context: Context) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.recurring_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun show(item: RecurringItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel()
        val reviewIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setData(identity(item, "review"))
                .putExtra(EXTRA_RECURRING_ID, item.id)
                .putExtra(EXTRA_DUE_AT, item.nextDueAtEpochMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.recurring_title)).setContentText(context.getString(R.string.open_review)).build())
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(item.description)
            .setContentText(MoneyFormatter.formatMinorUnits(item.amountMinor))
            .setContentIntent(reviewIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.skip), action(item, ReminderReceiver.ACTION_SKIP, 2))
            .addAction(0, context.getString(R.string.disable), action(item, ReminderReceiver.ACTION_DISABLE, 3))
        if (item.reminderStyle == com.hisabnikash.app.core.model.ReminderStyle.QUICK_CONFIRM) {
            notification.addAction(0, context.getString(R.string.confirm), action(item, ReminderReceiver.ACTION_CONFIRM, 1))
        }
        val built = notification.build()
        try {
            NotificationManagerCompat.from(context).notify(item.id, 0, built)
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and this call.
        }
    }

    private fun action(item: RecurringItem, action: String, offset: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        offset,
        Intent(context, ReminderReceiver::class.java).setAction(action)
            .setData(identity(item, action))
            .putExtra(EXTRA_RECURRING_ID, item.id)
            .putExtra(EXTRA_DUE_AT, item.nextDueAtEpochMillis),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun identity(item: RecurringItem, action: String): Uri = Uri.Builder()
        .scheme("hisabnikash").authority("recurring").appendPath(item.id)
        .appendPath(item.nextDueAtEpochMillis.toString()).appendPath(action).build()

    companion object {
        const val CHANNEL_ID = "recurring_reminders"
        const val EXTRA_RECURRING_ID = "recurring_id"
        const val EXTRA_DUE_AT = "recurring_due_at"
    }
}
