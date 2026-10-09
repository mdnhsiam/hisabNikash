package com.hisabnikash.app.feature.recurring

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.hisabnikash.app.HisabApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HisabApplication::class)
class ReminderDeletionTest {
    private lateinit var app: HisabApplication
    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
    }
    @After fun close() { app.container.database.close() }

    @Test fun `deleting local data removes already posted financial reminders`() = runTest {
        val item = recurring("delete-fixture", Long.MAX_VALUE / 2)
        app.container.recurringRepository.upsert(item)
        app.container.recurringScheduler.schedule(item)
        NotificationFactory(app).show(item)
        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(1, manager.activeNotifications.size)
        app.container.dataDeletionUseCase.deleteAllLocalData(true)
        assertEquals(0, manager.activeNotifications.size)
        val work = androidx.work.WorkManager.getInstance(app).getWorkInfosForUniqueWork(RecurringScheduler.uniqueName(item.id)).get()
        assertEquals(1, work.size)
        org.junit.Assert.assertTrue(work.all { it.state == androidx.work.WorkInfo.State.CANCELLED })
    }

    @Test fun `worker cannot publish a loaded reminder across the deletion boundary`() = runTest {
        val item = recurring("race-fixture", 0)
        app.container.recurringRepository.upsert(item)
        val manager = app.getSystemService(NotificationManager::class.java)
        val held = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val deletion = async(Dispatchers.IO) {
            app.container.backupOperationGate.run {
                held.complete(Unit)
                release.await()
                app.container.recurringRepository.clearAll()
            }
        }
        held.await()
        val worker = TestListenableWorkerBuilder<RecurringWorker>(app)
            .setInputData(Data.Builder().putString(RecurringWorker.KEY_RECURRING_ID, item.id).build()).build()
        val delivery = async(Dispatchers.IO) { worker.doWork() }
        val earlyCount: Int
        try {
            withContext(Dispatchers.Default) { withTimeoutOrNull(1_000) { delivery.await() } }
            earlyCount = manager.activeNotifications.size
        } finally { release.complete(Unit) }
        deletion.await()
        delivery.await()
        assertEquals("posting must wait for the shared deletion boundary", 0, earlyCount)
        assertEquals("a deleted item must not be posted after the boundary", 0, manager.activeNotifications.size)
    }
}
