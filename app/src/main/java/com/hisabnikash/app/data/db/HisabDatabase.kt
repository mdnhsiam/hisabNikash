package com.hisabnikash.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hisabnikash.app.data.db.dao.BudgetDao
import com.hisabnikash.app.data.db.dao.CategoryDao
import com.hisabnikash.app.data.db.dao.RecurringDao
import com.hisabnikash.app.data.db.dao.SavingsDao
import com.hisabnikash.app.data.db.dao.TransactionDao
import com.hisabnikash.app.data.db.entity.BudgetEntity
import com.hisabnikash.app.data.db.entity.CategoryEntity
import com.hisabnikash.app.data.db.entity.GoalAllocationEntity
import com.hisabnikash.app.data.db.entity.RecurringItemEntity
import com.hisabnikash.app.data.db.entity.SavingsGoalEntity
import com.hisabnikash.app.data.db.entity.TransactionEntity
import com.hisabnikash.app.data.repository.DefaultCategories

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        GoalAllocationEntity::class,
        RecurringItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DbConverters::class)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsDao(): SavingsDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        const val DATABASE_NAME = "hisab.db"

        fun create(context: Context): HisabDatabase =
            Room.databaseBuilder(context, HisabDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedCategoriesCallback)
                .build()

        fun createInMemory(context: Context): HisabDatabase =
            Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(SeedCategoriesCallback)
                .build()
    }
}

private object SeedCategoriesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DefaultCategories.all.forEach { category ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories
                (id, type, nameEn, nameBn, sortOrder, isSystem, isActive, createdAtEpochMillis)
                VALUES (?, ?, ?, ?, ?, 1, 1, 0)
                """.trimIndent(),
                arrayOf<Any>(category.id, category.type.name, category.nameEn, category.nameBn, category.sortOrder),
            )
        }
    }
}
