package com.spendtrack.app.domain.export

import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.model.TxnStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TransactionCsvTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    private fun txn(
        payee: String = "Ramesh Kumar",
        category: String? = "Food",
        note: String? = null,
        amount: Long = 125_050,
        status: TxnStatus = TxnStatus.CATEGORIZED,
        pkg: String = "com.phonepe.app",
    ) = TransactionEntity(
        id = 7,
        amountPaise = amount,
        payee = payee,
        category = category,
        note = note,
        timestamp = LocalDateTime.of(2026, 9, 5, 14, 3).atZone(ist).toInstant().toEpochMilli(),
        appPkg = pkg,
        rawText = "raw text is never exported",
        status = status,
    )

    private fun csv(vararg rows: TransactionEntity) = buildString { TransactionCsv.write(rows.toList(), this, ist) }

    @Test
    fun headerAndPlainRowUseCrlf() {
        assertEquals(
            "id,date,time,amount_inr,payee,category,status,source,note\r\n" +
                "7,2026-09-05,14:03,1250.50,Ramesh Kumar,Food,CATEGORIZED,PhonePe,\r\n",
            csv(txn()),
        )
    }

    @Test
    fun pendingManualAndIgnoredRows() {
        val lines = csv(
            txn(category = null, status = TxnStatus.PENDING),
            txn(pkg = "manual", payee = "Cash", amount = 5_000),
            txn(status = TxnStatus.IGNORED),
        ).split("\r\n")
        assertEquals("7,2026-09-05,14:03,1250.50,Ramesh Kumar,,PENDING,PhonePe,", lines[1])
        assertEquals("7,2026-09-05,14:03,50.00,Cash,Food,CATEGORIZED,Cash,", lines[2])
        assertEquals("7,2026-09-05,14:03,1250.50,Ramesh Kumar,Food,IGNORED,PhonePe,", lines[3])
    }

    @Test
    fun quotesCommasQuotesNewlinesAndPadding() {
        assertEquals("plain", TransactionCsv.escape("plain"))
        assertEquals("\"a, b\"", TransactionCsv.escape("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", TransactionCsv.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", TransactionCsv.escape("line1\nline2"))
        assertEquals("\" padded \"", TransactionCsv.escape(" padded "))
        assertEquals("", TransactionCsv.escape(""))
    }

    @Test
    fun neutralisesSpreadsheetFormulas() {
        val line = csv(txn(payee = "=HYPERLINK(\"x\")", note = "+91 phone", category = "@cat")).split("\r\n")[1]
        assertEquals("7,2026-09-05,14:03,1250.50,\"'=HYPERLINK(\"\"x\"\")\",'@cat,CATEGORIZED,PhonePe,'+91 phone", line)
        assertEquals("'-5", TransactionCsv.text("-5"))
        assertEquals("Food", TransactionCsv.text("Food"))
    }

    @Test
    fun plainAmounts() {
        assertEquals("0.00", Money.plain(0))
        assertEquals("0.05", Money.plain(5))
        assertEquals("100000.00", Money.plain(1_00_000_00))
        assertEquals("-12.30", Money.plain(-1_230))
    }
}
