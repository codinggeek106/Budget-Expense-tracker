package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.model.ParsedPayment

/**
 * Parses one UPI app's notifications. Must return null for anything that is not a successful
 * outgoing debit: failed, pending, processing, refund, cashback, received, request, promotional.
 *
 * Each implementation is written against the real samples in
 * `app/src/test/resources/samples/<app>.txt`; a format change should only touch that parser and its file.
 */
interface PaymentParser {
    val packageName: String
    fun parse(title: String?, text: String?, postedAt: Long): ParsedPayment?
}

/** The text kept in `TransactionEntity.rawText`: every non-blank part of the notification. */
internal fun rawTextOf(title: String?, text: String?): String =
    listOfNotNull(title, text).filter { it.isNotBlank() }.joinToString("\n")
