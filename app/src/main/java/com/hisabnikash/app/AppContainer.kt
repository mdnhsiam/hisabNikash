package com.hisabnikash.app

import android.content.Context
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.data.repository.RoomCategoryRepository
import com.hisabnikash.app.data.repository.RoomTransactionRepository
import com.hisabnikash.app.data.repository.TransactionRepository
import com.hisabnikash.app.data.repository.InMemoryPaymentMethodPreference
import com.hisabnikash.app.data.repository.PaymentMethodPreference
import com.hisabnikash.app.data.repository.BudgetRepository
import com.hisabnikash.app.data.repository.RoomBudgetRepository
import com.hisabnikash.app.data.repository.SavingsRepository
import com.hisabnikash.app.data.repository.RoomSavingsRepository
import com.hisabnikash.app.data.repository.RecurringRepository
import com.hisabnikash.app.data.repository.RoomRecurringRepository
import com.hisabnikash.app.feature.recurring.RecurringScheduler
import com.hisabnikash.app.feature.recurring.WorkManagerRecurringGateway
import androidx.room.withTransaction
import com.hisabnikash.app.data.preferences.UserPreferences
import com.hisabnikash.app.feature.settings.DataDeletionUseCase
import com.hisabnikash.app.backup.BackupScheduler
import com.hisabnikash.app.backup.SnapshotExporter
import com.hisabnikash.app.backup.SnapshotImporter
import com.hisabnikash.app.backup.WorkManagerBackupGateway
import com.hisabnikash.app.backup.crypto.AndroidBackupSecretStore
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.backup.local.DocumentTreeAccess
import com.hisabnikash.app.backup.local.LocalBackupStore
import com.hisabnikash.app.backup.drive.DriveAuthorizationClient
import com.hisabnikash.app.backup.drive.DriveBackupStore
import com.hisabnikash.app.backup.drive.DriveRestClient

class AppContainer(
    val applicationContext: Context,
) {
    val database: HisabDatabase by lazy { HisabDatabase.create(applicationContext) }
    val userPreferences: UserPreferences by lazy { UserPreferences.create(applicationContext) }
    val backupScheduler: BackupScheduler by lazy { BackupScheduler(WorkManagerBackupGateway(applicationContext)) }
    private val onDataChanged: () -> Unit = {
        backupScheduler.onDataChanged()
        backupScheduler.onCloudDataChanged()
    }
    val transactionRepository: TransactionRepository by lazy { RoomTransactionRepository(database, onDataChanged) }
    val categoryRepository: CategoryRepository by lazy { RoomCategoryRepository(database.categoryDao(), onDataChanged) }
    val paymentMethodPreference: PaymentMethodPreference by lazy { userPreferences }
    val budgetRepository: BudgetRepository by lazy { RoomBudgetRepository(database, onDataChanged) }
    val savingsRepository: SavingsRepository by lazy { RoomSavingsRepository(database, onDataChanged) }
    val recurringRepository: RecurringRepository by lazy { RoomRecurringRepository(database, transactionRepository, onDataChanged = onDataChanged) }
    val recurringScheduler: RecurringScheduler by lazy { RecurringScheduler(WorkManagerRecurringGateway(applicationContext)) }
    val backupPasswordCache: BackupPasswordCache by lazy { AndroidBackupSecretStore(applicationContext) }
    val snapshotExporter: SnapshotExporter by lazy { SnapshotExporter(database) }
    val snapshotImporter: SnapshotImporter by lazy { SnapshotImporter(database) }
    val localBackupStore: LocalBackupStore by lazy {
        LocalBackupStore(
            DocumentTreeAccess(applicationContext),
            userPreferences,
            snapshotExporter,
            snapshotImporter,
            backupPasswordCache,
        )
    }
    val driveAuthorizationClient: DriveAuthorizationClient by lazy { DriveAuthorizationClient(applicationContext) }
    val driveBackupStore: DriveBackupStore by lazy {
        DriveBackupStore(DriveRestClient(), snapshotExporter, snapshotImporter)
    }
    val dataDeletionUseCase: DataDeletionUseCase by lazy {
        DataDeletionUseCase(
            repositories = listOf(
                transactionRepository,
                budgetRepository,
                savingsRepository,
                recurringRepository,
                categoryRepository,
            ),
            clearPrivatePreferences = userPreferences::clearPrivateState,
            inTransaction = { operation -> database.withTransaction { operation() } },
        )
    }
}
