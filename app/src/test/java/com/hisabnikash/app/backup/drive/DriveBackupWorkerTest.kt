package com.hisabnikash.app.backup.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupWorkerTest {
    @Test
    fun `offline quota and server failures retry without affecting local ledger`() {
        assertTrue(DriveBackupWorkerPolicy.shouldRetry(DriveOfflineException()))
        assertTrue(DriveBackupWorkerPolicy.shouldRetry(DriveRetryableException(503)))
        assertTrue(DriveBackupWorkerPolicy.shouldRetry(DriveQuotaException()))
        assertFalse(DriveBackupWorkerPolicy.shouldRetry(DriveReauthorizationRequired()))
    }
}
