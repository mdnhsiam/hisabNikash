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
            item.id.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(EXTRA_RECURRING_ID, item.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(item.description)
            .setContentText(MoneyFormatter.formatMinorUnits(item.amountMinor))
            .setContentIntent(reviewIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.confirm), action(item.id, ReminderReceiver.ACTION_CONFIRM, 1))
            .addAction(0, context.getString(R.string.skip), action(item.id, ReminderReceiver.ACTION_SKIP, 2))
            .addAction(0, context.getString(R.string.disable), action(item.id, ReminderReceiver.ACTION_DISABLE, 3))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and this call.
        }
    }

    private fun action(id: String, action: String, offset: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode() + offset,
        Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_RECURRING_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "recurring_reminders"
        const val EXTRA_RECURRING_ID = "recurring_id"
    }
}
