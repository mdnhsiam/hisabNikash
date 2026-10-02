package com.hisabnikash.app.feature.recurring

import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.TransactionType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringSchedulerTest {
    @Test
    fun `monthly due date advances until it is in the future`() {
        val due = Instant.parse("2025-01-31T03:00:00Z").toEpochMilli()
        val after = Instant.parse("2025-03-01T00:00:00Z").toEpochMilli()

        assertEquals(Instant.parse("2025-03-28T03:00:00Z").toEpochMilli(), RecurringDatePolicy.nextDue(due, after))
    }

    @Test
    fun `schedule uses stable unique name and replacement prevents duplicate jobs`() {
        val gateway = FakeRecurringWorkGateway()
        val scheduler = RecurringScheduler(gateway, nowEpochMillis = { 100 })
        val item = recurring("bill", due = 1_000)

        scheduler.schedule(item)
        scheduler.schedule(item.copy(amountMinor = 200))

        assertEquals(1, gateway.jobs.size)
        assertEquals(900L, gateway.jobs.getValue("recurring-bill").delayMillis)
        scheduler.cancel("bill")
        assertTrue(gateway.jobs.isEmpty())
    }

    @Test
    fun `reboot reschedules only enabled reminders`() {
        val gateway = FakeRecurringWorkGateway()
        val scheduler = RecurringScheduler(gateway, nowEpochMillis = { 0 })

        scheduler.rescheduleAll(listOf(recurring("enabled", 100), recurring("disabled", 100).copy(isEnabled = false)))

        assertEquals(setOf("recurring-enabled"), gateway.jobs.keys)
    }

    @Test
    fun `denied notification permission or disabled item suppresses notification`() {
        assertFalse(NotificationPolicy.canPost(permissionGranted = false, itemEnabled = true, due = true))
        assertFalse(NotificationPolicy.canPost(permissionGranted = true, itemEnabled = false, due = true))
        assertTrue(NotificationPolicy.canPost(permissionGranted = true, itemEnabled = true, due = true))
    }
}

internal class FakeRecurringWorkGateway : RecurringWorkGateway {
    val jobs = linkedMapOf<String, ScheduledRecurringWork>()
    override fun enqueueUnique(work: ScheduledRecurringWork) { jobs[work.uniqueName] = work }
    override fun cancel(uniqueName: String) { jobs.remove(uniqueName) }
}

internal fun recurring(id: String, due: Long) = RecurringItem(
    id = id,
    type = TransactionType.EXPENSE,
    amountMinor = 100,
    description = "Electricity bill",
    categoryId = "expense_bills",
    paymentMethod = PaymentMethod.BKASH,
    nextDueAtEpochMillis = due,
    createdAtEpochMillis = 0,
)
