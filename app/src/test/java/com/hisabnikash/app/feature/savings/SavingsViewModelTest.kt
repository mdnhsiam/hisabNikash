package com.hisabnikash.app.feature.savings

import com.hisabnikash.app.core.model.GoalAllocation
import com.hisabnikash.app.core.model.SavingsGoal
import com.hisabnikash.app.data.repository.SavingsRepository
import com.hisabnikash.app.feature.transactions.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `creates goal allocates within available savings and records reversal`() = runTest {
        val repository = FakeSavingsRepository()
        val viewModel = SavingsViewModel(
            repository = repository,
            monthlySavingsMinor = MutableStateFlow(100_000),
            nowEpochMillis = { 100 },
            newId = sequenceOf("goal", "allocation", "reversal").iterator()::next,
        )
        advanceUntilIdle()

        assertTrue(viewModel.createGoal("Emergency", "5000") is SavingsActionResult.Saved)
        assertTrue(viewModel.allocate("goal", "600") is SavingsActionResult.Saved)
        advanceUntilIdle()
        assertEquals(60_000L, viewModel.state.value.allocatedMinor)
        assertEquals(40_000L, viewModel.state.value.availableMinor)

        assertTrue(viewModel.reverse("allocation") is SavingsActionResult.Saved)
        advanceUntilIdle()
        assertEquals(0L, viewModel.state.value.allocatedMinor)
        assertEquals("allocation", repository.allocations.value.last().reversesAllocationId)
    }

    @Test
    fun `cannot allocate more than positive unallocated savings`() = runTest {
        val repository = FakeSavingsRepository()
        repository.goals.value = listOf(SavingsGoal("goal", "Phone", null, createdAtEpochMillis = 0))
        val viewModel = SavingsViewModel(repository, MutableStateFlow(50_000), nowEpochMillis = { 100 }, newId = { "a" })
        advanceUntilIdle()

        assertTrue(viewModel.allocate("goal", "600") is SavingsActionResult.InsufficientSavings)
        assertTrue(repository.allocations.value.isEmpty())
    }
}

internal class FakeSavingsRepository : SavingsRepository {
    val goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val allocations = MutableStateFlow<List<GoalAllocation>>(emptyList())
    override fun observeGoals(): Flow<List<SavingsGoal>> = goals
    override fun observeAllocations(): Flow<List<GoalAllocation>> = allocations
    override suspend fun upsertGoal(goal: SavingsGoal) { goals.value = goals.value.filterNot { it.id == goal.id } + goal }
    override suspend fun archiveGoal(id: String, updatedAtEpochMillis: Long) {
        goals.value = goals.value.map { if (it.id == id) it.copy(isArchived = true, updatedAtEpochMillis = updatedAtEpochMillis) else it }
    }
    override suspend fun allocate(allocation: GoalAllocation) {
        require(allocation.amountMinor > 0)
        allocations.value = allocations.value + allocation
    }
    override suspend fun reverse(allocationId: String, reversalId: String, atEpochMillis: Long) {
        val original = allocations.value.first { it.id == allocationId }
        allocations.value = allocations.value + GoalAllocation(reversalId, original.goalId, -original.amountMinor, allocatedAtEpochMillis = atEpochMillis, reversesAllocationId = allocationId)
    }
}
