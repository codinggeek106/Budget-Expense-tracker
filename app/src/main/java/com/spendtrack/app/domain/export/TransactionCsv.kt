package com.spendtrack.app.domain.export

import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Transactions as RFC 4180 CSV (CRLF line endings, quoted where needed). Text cells that a
 * spreadsheet would treat as a formula are prefixed with an apostrophe.
 */
object TransactionCsv {

    val HEADER = listOf("id", "date", "time", "amount_inr", "payee", "category", "status", "source", "note")

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun write(transactions: List<TransactionEntity>, out: Appendable, zone: ZoneId = ZoneId.systemDefault()) {
        out.appendRow(HEADER)
        transactions.forEach { out.appendRow(row(it, zone)) }
    }

    fun row(txn: TransactionEntity, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        val time = Instant.ofEpochMilli(txn.timestamp).atZone(zone)
        return listOf(
            txn.id.toString(),
            dateFormat.format(time),
            timeFormat.format(time),
            Money.plain(txn.amountPaise),
            text(txn.payee),
            text(txn.category.orEmpty()),
            txn.status.name,
            text(UpiApps.label(txn.appPkg)),
            text(txn.note.orEmpty()),
        )
    }

    /** Neutralises spreadsheet formula injection in free-text cells. */
    fun text(value: String): String =
        if (value.isNotEmpty() && value[0] in FORMULA_PREFIXES) "'$value" else value

    fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' } || cell != cell.trim()) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }

    private fun Appendable.appendRow(cells: List<String>) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    private val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')
}
