package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.model.TxnStatus
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/** Records a cash payment entered by hand. It is stored already categorized. */
class AddManualTransaction(
    private val repository: TransactionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** Returns the new row id. [date] today uses the current time; other days use noon. */
    suspend operator fun invoke(
        amountPaise: Long,
        payee: String,
        category: String,
        note: String?,
        date: LocalDate,
    ): Long {
        require(amountPaise > 0) { "Amount must be positive" }
        require(category.isNotBlank()) { "Category is required" }

        val now = LocalDate.now(clock)
        val time = if (date == now) LocalTime.now(clock) else LocalTime.NOON
        return repository.insert(
            TransactionEntity(
                amountPaise = amountPaise,
                payee = payee.trim().ifEmpty { DEFAULT_PAYEE },
                category = Category.canonical(category, repository.getUsedCategories()),
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                timestamp = date.atTime(time).atZone(clock.zone).toInstant().toEpochMilli(),
                appPkg = TransactionEntity.MANUAL_PKG,
                rawText = "",
                status = TxnStatus.CATEGORIZED,
            )
        )
    }

    companion object {
        const val DEFAULT_PAYEE = "Cash"
    }
}
