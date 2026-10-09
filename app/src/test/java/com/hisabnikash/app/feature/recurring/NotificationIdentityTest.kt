package com.hisabnikash.app.feature.recurring

import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationIdentityTest {
    @Test fun `colliding hashes keep separate reminders and intents`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val factory = NotificationFactory(app)
        fun item(id: String) = RecurringItem(id, TransactionType.EXPENSE, 100, id, null, PaymentMethod.CASH,
            nextDueAtEpochMillis = 100, createdAtEpochMillis = 0)
        assertEquals("FB".hashCode(), "Ea".hashCode())
        factory.show(item("FB")); factory.show(item("Ea"))
        val notifications = app.getSystemService(NotificationManager::class.java).activeNotifications
        assertEquals(2, notifications.size)
        val ids = notifications.map { shadowOf(it.notification.contentIntent).savedIntent.getStringExtra(NotificationFactory.EXTRA_RECURRING_ID) }.toSet()
        assertEquals(setOf("FB", "Ea"), ids)
    }
}
