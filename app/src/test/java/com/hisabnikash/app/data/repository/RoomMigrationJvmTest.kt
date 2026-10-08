package com.hisabnikash.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.data.db.HisabDatabase
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomMigrationJvmTest {
    @Test fun `version one upgrades through both migrations and preserves records`() = verifyMigration(1)
    @Test fun `version two normalizes Bengali month keys without losing the budget`() = verifyMigration(2)

    private fun verifyMigration(version: Int) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-jvm-$version.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.hisabnikash.app.data.db.HisabDatabase/$version.json").readText()).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val entity = entities.getJSONObject(index)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.optJSONArray("indices") ?: entity.optJSONArray("indexes") ?: org.json.JSONArray()
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql")
                            .replace("\${TABLE_NAME}", entity.getString("tableName")))
                    }
                    val queries = schema.getJSONArray("setupQueries")
                    for (i in 0 until queries.length()) db.execSQL(queries.getString(i))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Fixture should not upgrade")
            }).build())
        helper.writableDatabase.execSQL("INSERT INTO budgets (id,monthKey,categoryId,amountMinor,createdAtEpochMillis,updatedAtEpochMillis) VALUES ('budget','২০২৫-০২',NULL,1000,0,0)")
        helper.close()
        val current = Room.databaseBuilder(context, HisabDatabase::class.java, name)
            .addMigrations(HisabDatabase.MIGRATION_1_2, HisabDatabase.MIGRATION_2_3).build()
        try {
            val budget = current.snapshotDao().budgets().single()
            assertEquals("2025-02", budget.monthKey)
            assertEquals(1000L, budget.amountMinor)
            assertEquals(3, current.openHelper.writableDatabase.version)
        } finally { current.close(); context.deleteDatabase(name) }
    }
}
