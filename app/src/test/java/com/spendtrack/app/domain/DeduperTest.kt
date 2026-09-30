package com.spendtrack.app.domain

import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.model.TxnStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeduperTest {

    private val t0 = 1_800_000_000_000L
    private val minute = 60_000L

    private fun payment(
        amount: Long = 25_000,
        payee: String = "Ramesh Kumar",
        at: Long = t0,
        raw: String = "raw-new",
    ) = ParsedPayment(amount, payee, at, "com.phonepe.app", raw)

    private fun stored(
        id: Long = 1,
        amount: Long = 25_000,
        payee: String = "Ramesh Kumar",
        at: Long = t0,
        raw: String = "raw-stored",
        status: TxnStatus = TxnStatus.PENDING,
    ) = TransactionEntity(id, amount, payee, timestamp = at, appPkg = "com.phonepe.app", rawText = raw, status = status)

    @Test
    fun sameAmountAndPayeeWithinTwoMinutesIsDuplicate() {
        val existing = stored(at = t0 - 2 * minute)
        assertEquals(existing, Deduper.findDuplicate(payment(), listOf(existing)))
    }

    @Test
    fun payeeComparisonIgnoresCaseAndSurroundingSpace() {
        val existing = stored(payee = "  ramesh KUMAR ")
        assertEquals(existing, Deduper.findDuplicate(payment(), listOf(existing)))
    }

    @Test
    fun sameAmountAndPayeeJustOutsideTwoMinutesIsNew() {
        val existing = stored(at = t0 - 2 * minute - 1)
        assertNull(Deduper.findDuplicate(payment(), listOf(existing)))
    }

    @Test
    fun differentAmountOrPayeeIsNew() {
        assertNull(Deduper.findDuplicate(payment(), listOf(stored(amount = 25_001))))
        assertNull(Deduper.findDuplicate(payment(), listOf(stored(payee = "Suresh"))))
    }

    @Test
    fun sameRawTextWithinTenMinutesIsDuplicate() {
        val existing = stored(amount = 1, payee = "other", at = t0 + 10 * minute, raw = "same text")
        assertEquals(existing, Deduper.findDuplicate(payment(raw = "same text"), listOf(existing)))
    }

    @Test
    fun sameRawTextAfterTenMinutesIsNew() {
        val existing = stored(at = t0 - 10 * minute - 1, raw = "same text")
        assertNull(Deduper.findDuplicate(payment(raw = "same text"), listOf(existing)))
    }

    @Test
    fun ignoredRowsStillCountAsDuplicates() {
        val existing = stored(status = TxnStatus.IGNORED)
        assertEquals(existing, Deduper.findDuplicate(payment(), listOf(existing)))
    }

    @Test
    fun noCandidatesMeansNew() {
        assertNull(Deduper.findDuplicate(payment(), emptyList()))
    }

    @Test
    fun rawTextHashIsStableAndDistinguishesText() {
        assertEquals(Deduper.rawTextHash("a"), Deduper.rawTextHash("a"))
        assertNotEquals(Deduper.rawTextHash("a"), Deduper.rawTextHash("b"))
        assertEquals(64, Deduper.rawTextHash("₹250").length)
    }
}
