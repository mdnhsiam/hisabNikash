package com.hisabnikash.app.backup.local

import com.hisabnikash.app.backup.BackupScheduler
import com.hisabnikash.app.backup.BackupWorkGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupWorkerTest {
    @Test
    fun `rapid data changes coalesce under one unique work name`() {
        val gateway = RecordingGateway()
        val scheduler = BackupScheduler(gateway)

        repeat(5) { scheduler.onDataChanged() }

        assertEquals(List(5) { BackupScheduler.UNIQUE_LOCAL_BACKUP }, gateway.names)
    }

    @Test
    fun `worker retries unavailable storage but not missing opt-in or password`() {
        assertTrue(LocalBackupWorkerPolicy.shouldRetry(BackupLocationUnavailableException()))
        assertFalse(LocalBackupWorkerPolicy.shouldRetry(BackupNotEnabledException()))
        assertFalse(LocalBackupWorkerPolicy.shouldRetry(BackupPasswordUnavailableException()))
    }

    private class RecordingGateway : BackupWorkGateway {
        val names = mutableListOf<String>()
        override fun enqueueUnique(name: String) { names += name }
    }
}
