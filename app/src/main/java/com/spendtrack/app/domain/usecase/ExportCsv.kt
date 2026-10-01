package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.export.TransactionCsv
import com.spendtrack.app.domain.model.MonthRange
import java.io.OutputStream
import java.time.ZoneId

class ExportCsv(private val repository: TransactionRepository) {

    /**
     * Writes [range] (or everything, if null) to [out] as UTF-8 CSV and closes it.
     * Returns the number of transactions written.
     */
    suspend operator fun invoke(range: MonthRange?, out: OutputStream, zone: ZoneId = ZoneId.systemDefault()): Int {
        val rows = if (range == null) repository.getAll() else repository.getMonth(range)
        out.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(BOM) // lets Excel detect UTF-8 (payee names, ₹ in notes)
            TransactionCsv.write(rows, writer, zone)
        }
        return rows.size
    }

    private companion object {
        const val BOM = "﻿"
    }
}
