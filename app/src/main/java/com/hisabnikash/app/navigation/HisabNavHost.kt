package com.hisabnikash.app.navigation

import android.Manifest
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.hisabnikash.app.feature.recurring.RecurringScreen
import com.hisabnikash.app.feature.recurring.RecurringViewModel
import com.hisabnikash.app.feature.settings.SettingsScreen
import com.hisabnikash.app.feature.settings.SettingsViewModel
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.domain.FinanceCalculator
import java.time.Instant
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabNavHost(
    container: AppContainer,
    requestedRecurringId: String? = null,
    onRecurringRequestConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPath = backStackEntry?.destination?.route ?: Route.Dashboard.path
    var showQuickEntry by remember { mutableStateOf(false) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentMonth = remember { MonthKey.fromInstant(Instant.now()) }
    val context = LocalContext.current
    val addTransactionLabel = stringResource(R.string.add_transaction)
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionGranted = granted
    }
    var restoreDocumentUri by remember { mutableStateOf<String?>(null) }
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
            viewModelFactory { initializer { TransactionListViewModel(container.transactionRepository, container.categoryRepository) } }
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
            viewModelFactory { initializer { ReportsViewModel(container.transactionRepository, container.categoryRepository) } }
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
                            transactions.sumOf { transaction ->
                                if (transaction.type == com.hisabnikash.app.core.model.TransactionType.INCOME) transaction.amountMinor else -transaction.amountMinor
                            }
                        },
                    )
                }
            }
        },
    )
    val recurringViewModel: RecurringViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer { RecurringViewModel(container.recurringRepository, container.recurringScheduler) }
            }
        },
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(container) {
            viewModelFactory {
                initializer {
                    SettingsViewModel(
                        container.userPreferences,
                        container.dataDeletionUseCase,
                        container.localBackupStore,
                        container.driveAuthorizationClient,
                        container.driveBackupStore,
                        container.driveBackupPasswordCache,
                        container.preRestoreBackupStore,
                        container.categoryRepository,
                    )
                }
            }
        },
    )
    val backupFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(settingsViewModel::chooseBackupLocation)
    }
    val restoreDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreDocumentUri = uri?.toString()
    }
    LaunchedEffect(requestedRecurringId) {
        if (requestedRecurringId != null) navController.navigate(Route.Settings.path) { launchSingleTop = true }
    }
    val driveResolution by settingsViewModel.driveResolution.collectAsState()
    val driveAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        settingsViewModel.completeDriveAuthorization(if (result.resultCode == Activity.RESULT_OK) result.data else null)
    }
    LaunchedEffect(driveResolution) {
        driveResolution?.let { pendingIntent ->
            driveAuthorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

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
            FloatingActionButton(
                onClick = { quickEntryViewModel.startNewEntry(); showQuickEntry = true },
                modifier = Modifier.semantics { contentDescription = addTransactionLabel },
            ) {
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
                        onSave = budgetViewModel::saveBudget,
                        onDelete = budgetViewModel::delete,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SavingsScreen(
                        state = savingsState,
                        onCreateGoal = savingsViewModel::createGoal,
                        onAllocate = savingsViewModel::allocate,
                        onReverse = savingsViewModel::reverse,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
            composable(Route.Settings.path) {
                val recurringState by recurringViewModel.state.collectAsState()
                val settingsState by settingsViewModel.state.collectAsState()
                val backupMessage by settingsViewModel.backupMessage.collectAsState()
                val settingsCategories by settingsViewModel.categories.collectAsState()
                SettingsScreen(
                    state = settingsState,
                    onLanguageChange = { language ->
                        settingsViewModel.setLanguage(language) { (context as? Activity)?.recreate() }
                    },
                    onDismissGuestWarning = settingsViewModel::dismissGuestWarning,
                    onDeleteAllData = settingsViewModel::deleteAllLocalData,
                    backupMessageRes = backupMessage,
                    restoreDocumentUri = restoreDocumentUri,
                    onChooseBackupLocation = { backupFolderLauncher.launch(null) },
                    onBackUpNow = settingsViewModel::backUpNow,
                    onChooseRestoreDocument = { restoreDocumentLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
                    onRestore = { uri, password -> settingsViewModel.restore(uri.toUri(), password) },
                    onDismissRestore = { restoreDocumentUri = null },
                    onDisableLocalBackup = settingsViewModel::disableLocalBackup,
                    onConnectDrive = { (context as? Activity)?.let(settingsViewModel::authorizeDrive) },
                    onDriveBackUpNow = settingsViewModel::driveBackUpNow,
                    onDriveRestoreLatest = settingsViewModel::driveRestoreLatest,
                    onDisconnectDrive = settingsViewModel::disconnectDrive,
                    showUndoRestore = settingsViewModel.hasRestoreRecovery(),
                    onUndoRestore = settingsViewModel::undoLastRestore,
                    categories = settingsCategories,
                    onRenameCategory = settingsViewModel::renameCategory,
                    onArchiveCategory = settingsViewModel::archiveCategory,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    recurringContent = {
                        RecurringScreen(
                            state = recurringState,
                            requestedReviewId = requestedRecurringId,
                            onReviewRequestConsumed = onRecurringRequestConsumed,
                            categories = settingsCategories,
                            notificationPermissionGranted = notificationPermissionGranted,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onCreate = recurringViewModel::create,
                            onConfirm = { id -> scope.launch { recurringViewModel.quickConfirm(id) } },
                            onReviewConfirm = { id, editor -> scope.launch { recurringViewModel.confirmWithEdits(id, editor) } },
                            onSkip = { id -> scope.launch { recurringViewModel.skip(id) } },
                            onDisable = { id -> scope.launch { recurringViewModel.disable(id) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
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
