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
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
