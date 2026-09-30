package com.spendtrack.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val september = MonthRange(YearMonth.of(2026, 9), ist)

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() = db.close()

    private fun at(month: Int, day: Int, hour: Int = 12) =
        LocalDateTime.of(2026, month, day, hour, 0).atZone(ist).toInstant().toEpochMilli()

    private fun txn(
        amount: Long,
        timestamp: Long,
        category: String? = null,
        status: TxnStatus = if (category == null) TxnStatus.PENDING else TxnStatus.CATEGORIZED,
        payee: String = "Payee",
    ) = TransactionEntity(
        amountPaise = amount,
        payee = payee,
        category = category,
        timestamp = timestamp,
        appPkg = "com.phonepe.app",
        rawText = "raw $amount $timestamp",
        status = status,
    )

    @Test
    fun insertAndReadBack() = runTest {
        val row = txn(25_000, at(9, 10), category = "Food").copy(note = "lunch")
        val id = dao.insert(row)
        assertEquals(row.copy(id = id), dao.getById(id))
        assertNull(dao.getById(id + 1))
    }

    @Test
    fun monthRangeIncludesStartAndExcludesEnd() = runTest {
        dao.insert(txn(1, september.startMillis - 1, payee = "aug-last-ms"))
        dao.insert(txn(2, september.startMillis, payee = "sep-first-ms"))
        dao.insert(txn(3, at(9, 15), payee = "sep-mid"))
        dao.insert(txn(4, september.endMillis - 1, payee = "sep-last-ms"))
        dao.insert(txn(5, september.endMillis, payee = "oct-first-ms"))

        val rows = dao.observeInRange(september.startMillis, september.endMillis).first()
        assertEquals(listOf("sep-last-ms", "sep-mid", "sep-first-ms"), rows.map { it.payee })
    }

    @Test
    fun monthRangeIncludesEveryStatus() = runTest {
        dao.insert(txn(1, at(9, 1), status = TxnStatus.PENDING))
        dao.insert(txn(2, at(9, 2), category = "Food"))
        dao.insert(txn(3, at(9, 3), category = "Food", status = TxnStatus.IGNORED))
        assertEquals(3, dao.observeInRange(september.startMillis, september.endMillis).first().size)
    }

    @Test
    fun categoryTotalsSumPerCategoryAndSkipIgnored() = runTest {
        dao.insert(txn(10_000, at(9, 1), category = "Food"))
        dao.insert(txn(15_050, at(9, 2), category = "Food"))
        dao.insert(txn(50_000, at(9, 3), category = "Bills"))
        dao.insert(txn(99_999, at(9, 4), category = "Bills", status = TxnStatus.IGNORED))
        dao.insert(txn(7_000, at(9, 5))) // pending
        dao.insert(txn(80_000, at(8, 31), category = "Food")) // previous month
        dao.insert(txn(80_000, at(10, 1), category = "Bills")) // next month

        val totals = dao.observeCategoryTotals(september.startMillis, september.endMillis).first()
        assertEquals(
            listOf(
                CategoryTotal("Bills", 50_000, 1),
                CategoryTotal("Food", 25_050, 2),
                CategoryTotal(null, 7_000, 1),
            ),
            totals,
        )
        assertEquals(82_050L, dao.observeTotal(september.startMillis, september.endMillis).first())
        assertEquals(totals.sumOf { it.totalPaise }, dao.observeTotal(september.startMillis, september.endMillis).first())
    }

    @Test
    fun emptyMonthTotalsAreZero() = runTest {
        assertEquals(emptyList<CategoryTotal>(), dao.observeCategoryTotals(september.startMillis, september.endMillis).first())
        assertEquals(0L, dao.observeTotal(september.startMillis, september.endMillis).first())
    }

    @Test
    fun timeWindowIsInclusive() = runTest {
        dao.insert(txn(1, 1_000))
        dao.insert(txn(2, 2_000))
        dao.insert(txn(3, 3_001))
        assertEquals(listOf(1L, 2L), dao.getInTimeWindow(1_000, 3_000).map { it.amountPaise }.sorted())
    }

    @Test
    fun categorizeSetsCategoryStatusAndKeepsNoteUnlessGiven() = runTest {
        val id = dao.insert(txn(1, at(9, 1)).copy(note = "keep me"))
        assertEquals(1, dao.categorize(id, "Travel"))
        dao.getById(id)!!.let {
            assertEquals("Travel", it.category)
            assertEquals(TxnStatus.CATEGORIZED, it.status)
            assertEquals("keep me", it.note)
        }
        dao.categorize(id, "Food", note = "new note")
        assertEquals("new note", dao.getById(id)!!.note)
        assertEquals(0, dao.categorize(id + 100, "Food"))
    }

    @Test
    fun pendingQueriesTrackStatusChanges() = runTest {
        val a = dao.insert(txn(1, at(9, 1)))
        val b = dao.insert(txn(2, at(9, 2)))
        dao.insert(txn(3, at(9, 3), category = "Food"))
        assertEquals(listOf(b, a), dao.observePending().first().map { it.id })
        assertEquals(2, dao.observePendingCount().first())

        dao.setStatus(a, TxnStatus.IGNORED)
        assertEquals(listOf(b), dao.observePending().first().map { it.id })
        assertEquals(1, dao.observePendingCount().first())
        assertFalse(dao.observePending().first().any { it.status != TxnStatus.PENDING })
    }
}
