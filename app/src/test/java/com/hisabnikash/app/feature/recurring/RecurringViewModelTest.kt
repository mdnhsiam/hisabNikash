package com.hisabnikash.app.feature.recurring

import com.hisabnikash.app.core.model.RecurringItem
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.data.repository.RecurringRepository
import com.hisabnikash.app.feature.transactions.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `quick confirm and edited confirm require explicit action then advance due`() = runTest {
        val repository = FakeRecurringRepository()
        repository.items.value = listOf(recurring("bill", 100))
        val viewModel = RecurringViewModel(repository, RecurringScheduler(FakeRecurringWorkGateway()) { 0 })
        advanceUntilIdle()

        assertEquals(0, repository.confirmed.size)
        viewModel.quickConfirm("bill")
        assertEquals(1, repository.confirmed.size)
        assertTrue(repository.items.value.single().nextDueAtEpochMillis > 100)

        val editor = viewModel.openForReview("bill")
        assertNotNull(editor)
        viewModel.confirmWithEdits("bill", editor!!.copy(description = "Edited bill"))
        assertEquals("Edited bill", repository.confirmed.last().description)
    }

    @Test
    fun `skip advances without transaction and disable cancels work`() = runTest {
        val repository = FakeRecurringRepository()
        val gateway = FakeRecurringWorkGateway()
        val scheduler = RecurringScheduler(gateway) { 0 }
        repository.items.value = listOf(recurring("bill", 100))
        val viewModel = RecurringViewModel(repository, scheduler)
        advanceUntilIdle()
        scheduler.schedule(repository.items.value.single())

        viewModel.skip("bill")
        assertTrue(repository.items.value.single().nextDueAtEpochMillis > 100)
        assertTrue(repository.confirmed.isEmpty())

        viewModel.disable("bill")
        assertFalse(repository.items.value.single().isEnabled)
        assertTrue(gateway.jobs.isEmpty())
    }
}

internal class FakeRecurringRepository : RecurringRepository {
    val items = MutableStateFlow<List<RecurringItem>>(emptyList())
    val confirmed = mutableListOf<Transaction>()
    override fun observeAll(): Flow<List<RecurringItem>> = items
    override suspend fun find(id: String): RecurringItem? = items.value.firstOrNull { it.id == id }
    override suspend fun upsert(item: RecurringItem) { items.value = items.value.filterNot { it.id == item.id } + item }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun confirmOccurrence(id: String, edits: RecurringEditor?): Transaction {
        val item = find(id) ?: error("missing")
        val transaction = item.toTransaction(edits, createdAtEpochMillis = 1_000)
        confirmed += transaction
        upsert(item.copy(nextDueAtEpochMillis = RecurringDatePolicy.nextDue(item.nextDueAtEpochMillis, 1_000)))
        return transaction
    }
    override suspend fun skipOccurrence(id: String) {
        val item = find(id) ?: error("missing")
        upsert(item.copy(nextDueAtEpochMillis = RecurringDatePolicy.nextDue(item.nextDueAtEpochMillis, 1_000)))
    }
    override suspend fun disable(id: String) {
        val item = find(id) ?: error("missing")
        upsert(item.copy(isEnabled = false))
    }
}
