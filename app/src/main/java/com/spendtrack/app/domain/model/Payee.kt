package com.spendtrack.app.domain.model

object Payee {
    /** Key for comparing payees: surrounding whitespace removed, lowercased. */
    fun normalize(payee: String): String = payee.trim().lowercase()
}
