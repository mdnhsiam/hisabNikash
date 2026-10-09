package com.hisabnikash.app.feature.settings

import com.hisabnikash.app.data.repository.ClearableRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataDeletionUseCaseTest {
    @Test
    fun `backup preferences are cleared before deleting financial records`() = runTest {
        var backupsEnabled = true
        val repository = object : ClearableRepository {
            override suspend fun clearAll() {
                assertFalse("deletion must prevent newly queued backup work before clearing the ledger", backupsEnabled)
            }
        }
        val useCase = DataDeletionUseCase(listOf(repository)) { backupsEnabled = false }

        assertTrue(useCase.deleteAllLocalData(confirmed = true))
    }
    @Test
    fun `unconfirmed deletion changes nothing`() = runTest {
        val stores = List(5) { RecordingStore() }
        var preferencesCleared = false
        val useCase = DataDeletionUseCase(stores) { preferencesCleared = true }

        assertFalse(useCase.deleteAllLocalData(confirmed = false))

        assertTrue(stores.none { it.cleared })
        assertFalse(preferencesCleared)
    }

    @Test
    fun `confirmed deletion clears every financial store and private preference`() = runTest {
        val stores = List(5) { RecordingStore() }
        var preferencesCleared = false
        val useCase = DataDeletionUseCase(stores) { preferencesCleared = true }

        assertTrue(useCase.deleteAllLocalData(confirmed = true))

        assertTrue(stores.all { it.cleared })
        assertTrue(preferencesCleared)
    }

    private class RecordingStore : ClearableRepository {
        var cleared = false
        override suspend fun clearAll() {
            cleared = true
        }
    }
}
