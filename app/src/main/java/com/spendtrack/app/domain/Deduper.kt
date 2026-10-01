package com.spendtrack.app.domain

import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.model.Payee
import java.security.MessageDigest
import kotlin.math.abs

/**
 * UPI apps often re-post or update the same notification. A payment is a duplicate of a stored
 * transaction (any status) if either:
 * - same amount and same normalized payee within [SAME_PAYMENT_WINDOW_MS], or
 * - identical raw text within [SAME_TEXT_WINDOW_MS].
 */
object Deduper {

    const val SAME_PAYMENT_WINDOW_MS = 2 * 60 * 1000L
    const val SAME_TEXT_WINDOW_MS = 10 * 60 * 1000L

    /** How far either side of a payment to fetch candidates from the database. */
    const val LOOKUP_WINDOW_MS = SAME_TEXT_WINDOW_MS

    fun normalizePayee(payee: String): String = Payee.normalize(payee)

    fun rawTextHash(rawText: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawText.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** The stored transaction [payment] duplicates, or null if it is new. */
    fun findDuplicate(payment: ParsedPayment, candidates: List<TransactionEntity>): TransactionEntity? {
        val payee = normalizePayee(payment.payee)
        val hash by lazy { rawTextHash(payment.rawText) }
        return candidates.firstOrNull { candidate ->
            val gap = abs(candidate.timestamp - payment.timestamp)
            val samePayment = gap <= SAME_PAYMENT_WINDOW_MS &&
                candidate.amountPaise == payment.amountPaise &&
                normalizePayee(candidate.payee) == payee
            samePayment || (gap <= SAME_TEXT_WINDOW_MS && rawTextHash(candidate.rawText) == hash)
        }
    }
}
