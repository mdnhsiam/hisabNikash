package com.hisabnikash.app.feature.recurring

import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.hisabnikash.app.HisabApplication
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringWorkerTest {
    @Test
    fun `worker decision only posts a reminder and never commits money`() {
        assertEquals(RecurringWorkerDecision.POST_REMINDER, RecurringWorkerPolicy.decide(permissionGranted = true, enabled = true, isDue = true))
        assertEquals(RecurringWorkerDecision.NO_OP, RecurringWorkerPolicy.decide(permissionGranted = false, enabled = true, isDue = true))
        assertEquals(RecurringWorkerDecision.NO_OP, RecurringWorkerPolicy.decide(permissionGranted = true, enabled = true, isDue = false))
    }

    @Test
    fun `denied notification worker exits successfully without writing transaction`() = runTest {
        val application = ApplicationProvider.getApplicationContext<HisabApplication>()
        val item = recurring("worker-denied", due = 1)
        application.container.recurringRepository.upsert(item)
        val worker = TestListenableWorkerBuilder<RecurringWorker>(application)
            .setInputData(Data.Builder().putString(RecurringWorker.KEY_RECURRING_ID, item.id).build())
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(application.container.transactionRepository.observeAll().first().none { it.description == item.description })
    }
}
