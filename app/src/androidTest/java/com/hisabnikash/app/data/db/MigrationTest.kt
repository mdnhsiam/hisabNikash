package com.hisabnikash.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HisabDatabase::class.java,
    )

    @Test
    fun versionOneSchemaCanBeCreatedFromExport() {
        helper.createDatabase(TEST_DATABASE, 1).close()
        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, HisabDatabase.MIGRATION_1_2, HisabDatabase.MIGRATION_2_3).close()
    }

    @Test
    fun versionTwoBanglaBudgetKeyIsNormalized() {
        helper.createDatabase(TEST_DATABASE_TWO, 2).apply {
            execSQL("INSERT INTO budgets (id, monthKey, categoryId, amountMinor, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('budget', '২০২৫-০২', NULL, 1000, 0, 0)")
            close()
        }
        helper.runMigrationsAndValidate(TEST_DATABASE_TWO, 3, true, HisabDatabase.MIGRATION_2_3).use { database ->
            database.query("SELECT monthKey FROM budgets WHERE id = 'budget'").use { cursor ->
                org.junit.Assert.assertTrue(cursor.moveToFirst())
                org.junit.Assert.assertEquals("2025-02", cursor.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
        const val TEST_DATABASE_TWO = "migration-test-two"
    }
}
