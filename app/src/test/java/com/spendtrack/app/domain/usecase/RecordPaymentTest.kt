package com.spendtrack.app.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.inMemoryDatabase
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecordPaymentTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val t0 = Instant.parse("2026-09-15T06:30:00Z").toEpochMilli()
    private val minute = 60_000L

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var recordPayment: RecordPayment

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = TransactionRepository(db.transactionDao(), db.budgetDao())
        recordPayment = RecordPayment(repository)
    }

    @After
    fun tearDown() = db.close()

    private fun payment(
        amount: Long = 25_000,
        payee: String = "Ramesh Kumar",
        at: Long = t0,
        raw: String = "Paid $amount to $payee at $at",
    ) = ParsedPayment(amount, payee, at, "com.phonepe.app", raw)

    private suspend fun allRows() = repository.observeMonth(MonthRange(YearMonth.of(2026, 9), ist)).first()

    @Test
    fun recordsNewPaymentAsPending() = runTest {
        val result = recordPayment(payment(payee = "  Ramesh Kumar "))
        val id = (result as RecordPayment.Result.Recorded).id
        val row = repository.getById(id)!!
        assertEquals(25_000L, row.amountPaise)
        assertEquals("Ramesh Kumar", row.payee)
        assertEquals(TxnStatus.PENDING, row.status)
        assertEquals(null, row.category)
        assertEquals("com.phonepe.app", row.appPkg)
        assertEquals(t0, row.timestamp)
    }

    @Test
    fun rejectsRepostOfSamePaymentWithinTwoMinutes() = runTest {
        val first = recordPayment(payment()) as RecordPayment.Result.Recorded
        val second = recordPayment(payment(payee = "RAMESH KUMAR", at = t0 + 90_000, raw = "different text"))
        assertEquals(RecordPayment.Result.Duplicate(first.id), second)
        assertEquals(1, allRows().size)
    }

    @Test
    fun rejectsIdenticalRawTextWithinTenMinutes() = runTest {
        recordPayment(payment(raw = "same"))
        val second = recordPayment(payment(amount = 1, payee = "x", at = t0 + 9 * minute, raw = "same"))
        assertTrue(second is RecordPayment.Result.Duplicate)
        assertEquals(1, allRows().size)
    }

    @Test
    fun keepsGenuineRepeatPayments() = runTest {
        recordPayment(payment())
        // Same payee and amount three minutes later (e.g. two coffees) is a new payment.
        assertTrue(recordPayment(payment(at = t0 + 3 * minute)) is RecordPayment.Result.Recorded)
        // Same payee within the window but a different amount is a new payment.
        assertTrue(recordPayment(payment(amount = 30_000, at = t0 + 30_000)) is RecordPayment.Result.Recorded)
        assertEquals(3, allRows().size)
    }

    @Test
    fun ignoredPaymentIsNotRecordedAgain() = runTest {
        val id = (recordPayment(payment()) as RecordPayment.Result.Recorded).id
        repository.setStatus(id, TxnStatus.IGNORED)
        assertEquals(RecordPayment.Result.Duplicate(id), recordPayment(payment(at = t0 + minute)))
    }

    @Test
    fun concurrentCopiesOfOneNotificationInsertOnce() = runTest {
        val results = withContext(Dispatchers.IO) {
            List(8) { async { recordPayment(payment()) } }.awaitAll()
        }
        assertEquals(1, results.count { it is RecordPayment.Result.Recorded })
        assertEquals(1, allRows().size)
    }
}
