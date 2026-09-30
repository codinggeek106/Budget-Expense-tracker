package com.spendtrack.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromptQueriesTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(timestamp: Long, category: String? = null, status: TxnStatus? = null) = dao.insert(
        TransactionEntity(
            amountPaise = 100,
            payee = "p",
            category = category,
            timestamp = timestamp,
            appPkg = "a",
            rawText = "r$timestamp",
            status = status ?: if (category == null) TxnStatus.PENDING else TxnStatus.CATEGORIZED,
        )
    )

    @Test
    fun topCategoriesByCountThenRecency() = runTest {
        add(1, "Food"); add(2, "Food"); add(3, "Food")
        add(4, "Bills"); add(5, "Bills")
        add(6, "Travel"); add(10, "Health")
        add(20, "Shopping", status = TxnStatus.IGNORED)
        add(21, "Shopping", status = TxnStatus.IGNORED)
        add(22, "Shopping", status = TxnStatus.IGNORED)

        assertEquals(listOf("Food", "Bills", "Health"), dao.getTopCategories(3))
    }

    @Test
    fun pendingOlderThanCutoffOnlyOldestFirst() = runTest {
        val old = add(1_000)
        val boundary = add(5_000)
        add(9_000) // too recent
        add(500, category = "Food") // not pending
        add(600, status = TxnStatus.IGNORED)

        assertEquals(listOf(old, boundary), dao.getPendingOlderThan(5_000).map { it.id })
    }
}
