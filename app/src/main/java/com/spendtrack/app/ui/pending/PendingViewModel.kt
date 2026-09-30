package com.spendtrack.app.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.usecase.Categorize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingViewModel(
    repository: TransactionRepository,
    private val categorize: Categorize,
) : ViewModel() {

    /** null until the first load finishes, so the screen can tell "loading" from "empty". */
    val pending: StateFlow<List<TransactionEntity>?> = repository.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ignore(txnId: Long) {
        viewModelScope.launch { categorize.ignore(txnId) }
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
