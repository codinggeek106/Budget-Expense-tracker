package com.spendtrack.app.domain.model

import com.spendtrack.app.data.db.BudgetEntity
import com.spendtrack.app.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthlySummaryTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val sep = MonthRange(YearMonth.of(2026, 9), ist)
    private var nextId = 1L

    private fun at(day: Int, month: Int = 9) =
        LocalDateTime.of(2026, month, day, 12, 0).atZone(ist).toInstant().toEpochMilli()

    private fun txn(
        amount: Long,
        category: String? = null,
        payee: String = "Payee",
        day: Int = 10,
        month: Int = 9,
        status: TxnStatus = if (category == null) TxnStatus.PENDING else TxnStatus.CATEGORIZED,
    ) = TransactionEntity(
        id = nextId++,
        amountPaise = amount,
        payee = payee,
        category = category,
        timestamp = at(day, month),
        appPkg = "a",
        rawText = "r",
        status = status,
    )

    @Test
    fun totalIsSumOfNonIgnoredRowsInTheMonth() {
        val rows = listOf(
            txn(10_000, "Food"),
            txn(20_050, "Bills"),
            txn(5_000), // pending counts
            txn(99_999, "Food", status = TxnStatus.IGNORED),
            txn(77_777, "Food", month = 8, day = 31), // outside the month
        )
        val s = MonthlySummary.from(sep, rows, emptyList())
        assertEquals(35_050L, s.totalPaise)
        assertEquals(3, s.paymentCount)
        assertEquals(5_000L, s.pendingPaise)
        assertEquals(1, s.pendingCount)
        assertEquals(s.totalPaise, s.categories.sumOf { it.totalPaise })
    }

    @Test
    fun categoriesAreSortedByTotalWithPendingAsNull() {
        val s = MonthlySummary.from(
            sep,
            listOf(txn(100, "Food"), txn(300, "Food"), txn(250, "Bills"), txn(1_000)),
            emptyList(),
        )
        assertEquals(
            listOf(CategorySpend(null, 1_000, 1), CategorySpend("Food", 400, 2), CategorySpend("Bills", 250, 1)),
            s.categories,
        )
    }

    @Test
    fun topPayeesMergeCaseAndKeepTopFive() {
        val rows = listOf(
            txn(100, "Food", payee = "Ramesh"),
            txn(200, "Food", payee = "ramesh "),
            txn(150, "Food", payee = "Ramesh"),
            txn(900, "Food", payee = "A"),
            txn(800, "Food", payee = "B"),
            txn(700, "Food", payee = "C"),
            txn(600, "Food", payee = "D"),
            txn(10, "Food", payee = "E"),
            txn(5_000, "Food", payee = "Ignored", status = TxnStatus.IGNORED),
        )
        val payees = MonthlySummary.from(sep, rows, emptyList()).topPayees
        assertEquals(listOf("A", "B", "C", "D", "Ramesh"), payees.map { it.payee })
        assertEquals(PayeeSpend("Ramesh", 450, 3), payees.last())
    }

    @Test
    fun budgetProgressUsesOnlyCategorizedSpendInThatCategory() {
        val rows = listOf(
            txn(30_000, "Food"),
            txn(20_000, "food"),
            txn(10_000), // pending: not attributed to any budget yet
            txn(40_000, "Food", status = TxnStatus.IGNORED),
            txn(1_000, "Bills"),
        )
        val budgets = listOf(BudgetEntity("Food", 40_000), BudgetEntity("Bills", 1_000), BudgetEntity("Travel", 5_000))
        val progress = MonthlySummary.from(sep, rows, budgets).budgets.associateBy { it.category }

        with(progress.getValue("Food")) {
            assertEquals(50_000L, spentPaise)
            assertEquals(1.25f, fraction, 0.0001f)
            assertTrue(isOver)
            assertEquals(-10_000L, remainingPaise)
        }
        with(progress.getValue("Bills")) {
            assertEquals(1f, fraction, 0.0001f)
            assertFalse("exactly 100% is not over", isOver)
            assertEquals(0L, remainingPaise)
        }
        with(progress.getValue("Travel")) {
            assertEquals(0L, spentPaise)
            assertEquals(0f, fraction, 0.0001f)
            assertFalse(isOver)
        }
    }

    @Test
    fun emptyMonth() {
        val s = MonthlySummary.from(sep, emptyList(), listOf(BudgetEntity("Food", 100)))
        assertEquals(0L, s.totalPaise)
        assertTrue(s.categories.isEmpty())
        assertTrue(s.topPayees.isEmpty())
        assertEquals(listOf(BudgetProgress("Food", 100, 0)), s.budgets)
    }
}
