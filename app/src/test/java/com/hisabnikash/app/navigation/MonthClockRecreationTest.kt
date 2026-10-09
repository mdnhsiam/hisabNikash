package com.hisabnikash.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hisabnikash.app.core.time.*
import com.hisabnikash.app.feature.dashboard.DashboardViewModel
import com.hisabnikash.app.feature.transactions.FakeTransactionRepository
import com.hisabnikash.app.feature.transactions.transaction
import java.time.Instant
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonthClockRecreationTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun close() { store.clear() }

    @Test fun `recreated composition refreshes the clock used by retained finance models`() {
        val january = MonthKey(2025, 1)
        var now = Instant.ofEpochMilli(january.startEpochMillis())
        var visible by mutableStateOf(true)
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        val repository = FakeTransactionRepository().apply {
            flow.value = listOf(transaction("jan").copy(amountMinor = 200, occurredAtEpochMillis = january.startEpochMillis()),
                transaction("feb").copy(amountMinor = 700, occurredAtEpochMillis = january.next().startEpochMillis()))
        }
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                if (visible) {
                    val clock = rememberMonthClock { CurrentMonthClock { now } }
                    val model: DashboardViewModel = viewModel(factory = remember {
                        viewModelFactory { initializer { DashboardViewModel(repository, currentMonth = { january }, months = clock.month) } }
                    })
                    LaunchedEffect(clock) { clock.refresh() }
                    val state by model.state.collectAsState()
                    Text(state.summary.expenseMinor.toString())
                }
            }
        }
        compose.onNodeWithText("200").assertExists()
        // Discard composition, just as recreation does, while retaining the Activity VM store.
        compose.runOnIdle { visible = false }
        compose.waitForIdle()
        compose.runOnIdle { visible = true }
        compose.waitForIdle()
        // Resume again after a Dhaka month boundary without a ledger emission.
        compose.runOnIdle { visible = false; now = Instant.ofEpochMilli(january.next().startEpochMillis()) }
        compose.waitForIdle()
        compose.runOnIdle { visible = true }
        compose.waitForIdle()
        compose.onNodeWithText("700").assertExists()
    }
}
