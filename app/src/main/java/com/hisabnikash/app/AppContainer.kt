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

class AppContainer(
    val applicationContext: Context,
) {
    val database: HisabDatabase by lazy { HisabDatabase.create(applicationContext) }
    val transactionRepository: TransactionRepository by lazy { RoomTransactionRepository(database) }
    val categoryRepository: CategoryRepository by lazy { RoomCategoryRepository(database.categoryDao()) }
    val paymentMethodPreference: PaymentMethodPreference = InMemoryPaymentMethodPreference()
    val budgetRepository: BudgetRepository by lazy { RoomBudgetRepository(database) }
    val savingsRepository: SavingsRepository by lazy { RoomSavingsRepository(database) }
    val recurringRepository: RecurringRepository by lazy { RoomRecurringRepository(database, transactionRepository) }
    val recurringScheduler: RecurringScheduler by lazy { RecurringScheduler(WorkManagerRecurringGateway(applicationContext)) }
}
