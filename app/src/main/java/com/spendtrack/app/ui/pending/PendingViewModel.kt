package com.spendtrack.app.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.Payee
import com.spendtrack.app.domain.usecase.Categorize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A pending payment plus the category last used for the same payee, if any. */
data class PendingItem(val txn: TransactionEntity, val suggestion: String?)

class PendingViewModel(
    private val repository: TransactionRepository,
    private val categorize: Categorize,
) : ViewModel() {

    /** null until the first load finishes, so the screen can tell "loading" from "empty". */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pending: StateFlow<List<PendingItem>?> = repository.observePending()
        .mapLatest { rows ->
            val suggestions = mutableMapOf<String, String?>()
            rows.map { txn ->
                val key = Payee.normalize(txn.payee)
                PendingItem(txn, suggestions.getOrPut(key) { repository.getLastCategoryForPayee(txn.payee) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ignore(txnId: Long) {
        viewModelScope.launch { categorize.ignore(txnId) }
    }

    fun applySuggestion(item: PendingItem) {
        val category = item.suggestion ?: return
        viewModelScope.launch { categorize(item.txn.id, category) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                PendingViewModel(container.transactionRepository, container.categorize)
            }
        }
    }
}
