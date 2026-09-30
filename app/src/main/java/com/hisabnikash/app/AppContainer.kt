package com.hisabnikash.app

import android.content.Context
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.data.repository.RoomCategoryRepository
import com.hisabnikash.app.data.repository.RoomTransactionRepository
import com.hisabnikash.app.data.repository.TransactionRepository

class AppContainer(
    val applicationContext: Context,
) {
    val database: HisabDatabase by lazy { HisabDatabase.create(applicationContext) }
    val transactionRepository: TransactionRepository by lazy { RoomTransactionRepository(database) }
    val categoryRepository: CategoryRepository by lazy { RoomCategoryRepository(database.categoryDao()) }
}
