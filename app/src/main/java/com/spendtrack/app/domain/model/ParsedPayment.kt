package com.spendtrack.app.domain.model

/** A successful outgoing UPI debit extracted from a notification. Money is in paise. */
data class ParsedPayment(
    val amountPaise: Long,
    val payee: String,
    val timestamp: Long,
    val appPkg: String,
    val rawText: String,
)
