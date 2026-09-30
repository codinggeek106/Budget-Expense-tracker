package com.spendtrack.app.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.data.db.inMemoryDatabase
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** Phase 5 acceptance: month totals match stored rows, and budget progress is right. */
@RunWith(AndroidJUnit4::class)
class ReportsTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val clock = Clock.fixed(Instant.parse("2026-09-20T09:30:00Z"), ist) // 20 Sep, 15:00 IST
    private val sep = MonthRange(YearMonth.of(2026, 9), ist)

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var summary: GetMonthlySummary
    private lateinit var setBudget: SetBudget
    private lateinit var addManual: AddManualTransaction
    private lateinit var record: RecordPayment
    private lateinit var categorize: Categorize

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = TransactionRepository(db.transactionDao(), db.budgetDao())
        summary = GetMonthlySummary(repository)
        setBudget = SetBudget(repository)
        addManual = AddManualTransaction(repository, clock)
        record = RecordPayment(repository)
        categorize = Categorize(repository) {}
    }

    @After
    fun tearDown() = db.close()

    private fun at(day: Int, month: Int = 9) =
        LocalDateTime.of(2026, month, day, 10, 0).atZone(ist).toInstant().toEpochMilli()

    private suspend fun upi(amount: Long, payee: String, day: Int, month: Int = 9): Long =
        (record(ParsedPayment(amount, payee, at(day, month), "com.phonepe.app", "raw $payee $day $month")) as RecordPayment.Result.Recorded).id

    @Test
    fun monthTotalMatchesSumOfStoredTransactions() = runTest {
        categorize(upi(25_000, "Cafe", 1), "Food")
        categorize(upi(1_20_000, "Electricity", 5), "Bills")
        upi(9_900, "Unknown", 7) // stays pending
        categorize.ignore(upi(50_000, "Refund test", 8))
        addManual(15_050, "Vendor", "Groceries", null, LocalDate.of(2026, 9, 9))
        upi(1_000_00, "August", 31, month = 8)
        upi(2_000_00, "October", 1, month = 10)

        val stored = repository.observeMonth(sep).first()
        val expected = stored.filter { it.status != TxnStatus.IGNORED }.sumOf { it.amountPaise }
        val s = summary(sep).first()

        assertEquals(25_000L + 1_20_000 + 9_900 + 15_050, expected)
        assertEquals(expected, s.totalPaise)
        assertEquals(expected, s.categories.sumOf { it.totalPaise })
        // Same figure the SQL aggregate reports.
        assertEquals(expected, repository.observeMonthTotal(sep).first())
        assertEquals(4, s.paymentCount)
        assertEquals(1, s.pendingCount)
    }

    @Test
    fun budgetProgressTracksCategorizedSpend() = runTest {
        setBudget("food", 30_000) // canonicalised to "Food"
        setBudget("Bills", 1_00_000)
        categorize(upi(20_000, "Cafe", 2), "Food")
        categorize(upi(15_000, "Dhaba", 3), "Food")
        categorize(upi(60_000, "Water", 4), "Bills")

        val budgets = summary(sep).first().budgets.associateBy { it.category }
        with(budgets.getValue("Food")) {
            assertEquals(30_000L, limitPaise)
            assertEquals(35_000L, spentPaise)
            assertTrue(isOver)
            assertEquals(35_000f / 30_000f, fraction, 0.0001f)
        }
        with(budgets.getValue("Bills")) {
            assertEquals(60_000L, spentPaise)
            assertFalse(isOver)
            assertEquals(0.6f, fraction, 0.0001f)
        }

        // Updating and removing budgets is reflected immediately.
        setBudget("Food", 50_000)
        setBudget("Bills", null)
        val updated = summary(sep).first().budgets
        assertEquals(listOf("Food"), updated.map { it.category })
        assertFalse(updated.single().isOver)
    }

    @Test
    fun manualEntryIsStoredCategorizedAsCash() = runTest {
        val today = addManual(12_345, "  ", " groceries ", "  milk ", LocalDate.of(2026, 9, 20))
        val earlier = addManual(500, "Auto", "Travel", null, LocalDate.of(2026, 9, 3))

        with(repository.getById(today)!!) {
            assertEquals(TransactionEntity.MANUAL_PKG, appPkg)
            assertEquals(TxnStatus.CATEGORIZED, status)
            assertEquals("Cash", payee)
            assertEquals("Groceries", category)
            assertEquals("milk", note)
            assertEquals(LocalTime.of(15, 0), Instant.ofEpochMilli(timestamp).atZone(ist).toLocalTime())
        }
        with(repository.getById(earlier)!!) {
            assertEquals(LocalDateTime.of(2026, 9, 3, 12, 0), Instant.ofEpochMilli(timestamp).atZone(ist).toLocalDateTime())
        }
        assertEquals(12_845L, summary(sep).first().totalPaise)
    }

    @Test
    fun unignoreRestoresPreviousStatus() = runTest {
        val categorized = upi(100, "A", 1).also { categorize(it, "Food") }
        val pending = upi(200, "B", 2)
        categorize.ignore(categorized)
        categorize.ignore(pending)

        repository.unignore(repository.getById(categorized)!!)
        repository.unignore(repository.getById(pending)!!)
        assertEquals(TxnStatus.CATEGORIZED, repository.getById(categorized)!!.status)
        assertEquals(TxnStatus.PENDING, repository.getById(pending)!!.status)
    }
}
