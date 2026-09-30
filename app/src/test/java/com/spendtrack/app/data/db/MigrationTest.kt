package com.spendtrack.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Phase 1 installs have a v1 database; upgrading must keep their captured samples.
 * Builds a real v1 file from the exported schema, then lets Room migrate it. Room validates the
 * migrated schema against the current entities on open, so a bad migration throws here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2KeepsRawNotifications() = runTest {
        createDatabaseFromSchema(version = 1).use { db ->
            db.execSQL(
                "INSERT INTO raw_notifications (pkg, title, text, bigText, postedAt) " +
                    "VALUES ('com.phonepe.app', 'title', 'text', NULL, 42)"
            )
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            assertEquals(listOf("title"), db.rawNotificationDao().observeAll().first().map { it.title })

            // New tables exist and work.
            val id = db.transactionDao().insert(
                TransactionEntity(amountPaise = 100, payee = "p", timestamp = 1, appPkg = "a", rawText = "r")
            )
            assertEquals(TxnStatus.PENDING, db.transactionDao().getById(id)!!.status)
            db.budgetDao().upsert(BudgetEntity("Food", 100))
            assertEquals(1, db.budgetDao().observeAll().first().size)
        } finally {
            db.close()
        }
    }

    private fun createDatabaseFromSchema(version: Int): SQLiteDatabase {
        val schemaDir = System.getProperty("room.schemaDir") ?: error("room.schemaDir not set")
        val schema = JSONObject(
            File(schemaDir, "${AppDatabase::class.java.name}/$version.json").readText()
        ).getJSONObject("database")

        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
        return db
    }
}
