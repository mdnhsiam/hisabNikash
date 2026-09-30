package com.hisabnikash.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hisabnikash.app.AppContainer
import com.hisabnikash.app.R
import com.hisabnikash.app.feature.transactions.QuickEntryScreen
import com.hisabnikash.app.feature.transactions.QuickEntryViewModel
import com.hisabnikash.app.feature.transactions.SubmitResult
import com.hisabnikash.app.feature.transactions.TransactionListScreen
import com.hisabnikash.app.feature.transactions.TransactionListViewModel
import com.hisabnikash.app.feature.dashboard.DashboardScreen
import com.hisabnikash.app.feature.dashboard.DashboardViewModel
import com.hisabnikash.app.feature.reports.ReportsScreen
import com.hisabnikash.app.feature.reports.ReportsViewModel
import com.hisabnikash.app.feature.budgets.BudgetScreen
import com.hisabnikash.app.feature.budgets.BudgetViewModel
import com.hisabnikash.app.feature.savings.SavingsScreen
import com.hisabnikash.app.feature.savings.SavingsViewModel
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.domain.FinanceCalculator
import java.time.Instant
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPath = backStackEntry?.destination?.route ?: Route.Dashboard.path
    var showQuickEntry by remember { mutableStateOf(false) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentMonth = remember { MonthKey.fromInstant(Instant.now()) }
    val quickEntryViewModel: QuickEntryViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer {
                    QuickEntryViewModel(
                        container.transactionRepository,
                        container.categoryRepository,
                        container.paymentMethodPreference,
                    )
                }
            }
        },
    )
    val transactionListViewModel: TransactionListViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory { initializer { TransactionListViewModel(container.transactionRepository) } }
        },
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer {
                    DashboardViewModel(
                        transactionRepository = container.transactionRepository,
                        budgets = container.budgetRepository.observeMonth(currentMonth),
                        allocations = container.savingsRepository.observeAllocations(),
                    )
                }
            }
        },
    )
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory { initializer { ReportsViewModel(container.transactionRepository) } }
        },
    )
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer {
                    BudgetViewModel(
                        repository = container.budgetRepository,
                        month = currentMonth,
                        categories = container.categoryRepository.observeActive(CategoryType.EXPENSE),
                    )
                }
            }
        },
    )
    val savingsViewModel: SavingsViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer {
                    SavingsViewModel(
                        repository = container.savingsRepository,
                        monthlySavingsMinor = container.transactionRepository.observeAll().map { transactions ->
                            FinanceCalculator.monthlyReport(transactions, currentMonth).netSavingsMinor
                        },
                    )
                }
            }
        },
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                Route.bottomRoutes.forEach { route ->
                    NavigationBarItem(
                        selected = currentPath == route.path,
                        onClick = {
                            navController.navigate(route.path) {
                                popUpTo(Route.Dashboard.path) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        },
                        label = { Text(stringResource(route.labelRes)) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickEntry = true }) {
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Dashboard.path) {
                val state by dashboardViewModel.state.collectAsState()
                DashboardScreen(state)
            }
            composable(Route.Transactions.path) {
                val state by transactionListViewModel.state.collectAsState()
                TransactionListScreen(
                    state = state,
                    onQueryChange = transactionListViewModel::setQuery,
                    onUpdate = transactionListViewModel::update,
                    onDelete = transactionListViewModel::delete,
                )
            }
            composable(Route.Reports.path) {
                val state by reportsViewModel.state.collectAsState()
                ReportsScreen(state, reportsViewModel::previousMonth, reportsViewModel::nextMonth)
            }
            composable(Route.Savings.path) {
                val budgetState by budgetViewModel.state.collectAsState()
                val savingsState by savingsViewModel.state.collectAsState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    BudgetScreen(
                        state = budgetState,
                        onSave = { amount, category -> scope.launch { budgetViewModel.saveBudget(amount, category) } },
                        onDelete = budgetViewModel::delete,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SavingsScreen(
                        state = savingsState,
                        onCreateGoal = { name, target -> scope.launch { savingsViewModel.createGoal(name, target) } },
                        onAllocate = { goal, amount -> scope.launch { savingsViewModel.allocate(goal, amount) } },
                        onReverse = { allocation -> scope.launch { savingsViewModel.reverse(allocation) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
            composable(Route.Settings.path) { PlaceholderScreen(Route.Settings) }
        }
    }

    if (showQuickEntry) {
        val state by quickEntryViewModel.state.collectAsState()
        ModalBottomSheet(onDismissRequest = { showQuickEntry = false }) {
            QuickEntryScreen(
                state = state,
                onTypeChange = quickEntryViewModel::setType,
                onAmountChange = quickEntryViewModel::setAmount,
                onDescriptionChange = quickEntryViewModel::setDescription,
                onCategoryChange = quickEntryViewModel::setCategory,
                onCustomCategoryChange = quickEntryViewModel::setCustomCategory,
                onPaymentMethodChange = quickEntryViewModel::setPaymentMethod,
                onNoteChange = quickEntryViewModel::setNote,
                onSubmit = {
                    scope.launch {
                        when (quickEntryViewModel.submit()) {
                            is SubmitResult.Saved -> showQuickEntry = false
                            is SubmitResult.PossibleDuplicate -> showDuplicateWarning = true
                            else -> Unit
                        }
                    }
                },
            )
        }
    }

    if (showDuplicateWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarning = false },
            title = { Text(stringResource(R.string.possible_duplicate)) },
            text = { Text(stringResource(R.string.possible_duplicate_message)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (quickEntryViewModel.submit(intentionalDuplicate = true) is SubmitResult.Saved) {
                            showDuplicateWarning = false
                            showQuickEntry = false
                        }
                    }
                }) { Text(stringResource(R.string.save_anyway)) }
            },
            dismissButton = {
                Button(onClick = { showDuplicateWarning = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PlaceholderScreen(route: Route) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(route.labelRes), style = MaterialTheme.typography.headlineMedium)
    }
}
