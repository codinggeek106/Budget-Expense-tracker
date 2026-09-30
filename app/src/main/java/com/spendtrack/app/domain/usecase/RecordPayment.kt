package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.Deduper
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Stores a detected payment as PENDING unless it duplicates one already stored. */
class RecordPayment(private val repository: TransactionRepository) {

    sealed interface Result {
        data class Recorded(val id: Long) : Result
        data class Duplicate(val existingId: Long) : Result
    }

    /**
     * Serialises check-then-insert so two copies of a notification arriving together can't both
     * pass the duplicate check. Keep a single instance per process (see AppContainer).
     */
    private val mutex = Mutex()

    suspend operator fun invoke(payment: ParsedPayment): Result = mutex.withLock {
        val candidates = repository.transactionsAround(payment.timestamp, Deduper.LOOKUP_WINDOW_MS)
        Deduper.findDuplicate(payment, candidates)?.let { return Result.Duplicate(it.id) }

        val id = repository.insert(
            TransactionEntity(
                amountPaise = payment.amountPaise,
                payee = payment.payee.trim(),
                timestamp = payment.timestamp,
                appPkg = payment.appPkg,
                rawText = payment.rawText,
                status = TxnStatus.PENDING,
            )
        )
        Result.Recorded(id)
    }
}
