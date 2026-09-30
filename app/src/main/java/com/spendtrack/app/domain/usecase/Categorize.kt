package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.model.TxnStatus

/**
 * Categorizes or ignores a transaction, then dismisses its prompt notification.
 * [dismissPrompt] is injected so the domain layer doesn't depend on Android notifications.
 */
class Categorize(
    private val repository: TransactionRepository,
    private val dismissPrompt: (txnId: Long) -> Unit,
) {

    /** Returns false if [category] is blank or the transaction no longer exists. */
    suspend operator fun invoke(txnId: Long, category: String, note: String? = null): Boolean {
        if (category.isBlank()) return false
        val canonical = Category.canonical(category, repository.getUsedCategories())
        val updated = repository.categorize(txnId, canonical, note?.trim()?.takeIf { it.isNotEmpty() })
        dismissPrompt(txnId)
        return updated
    }

    suspend fun ignore(txnId: Long): Boolean {
        val updated = repository.setStatus(txnId, TxnStatus.IGNORED)
        dismissPrompt(txnId)
        return updated
    }
}
