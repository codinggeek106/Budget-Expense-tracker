package com.spendtrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.usecase.Categorize
import com.spendtrack.app.ui.components.MonthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryState(val range: MonthRange, val transactions: List<TransactionEntity>)

class HistoryViewModel(
    private val repository: TransactionRepository,
    private val categorize: Categorize,
) : ViewModel() {

    val month = MonthState()

    /** Every transaction in the selected month, any status, newest first. null while loading. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryState?> = month.range
        .flatMapLatest { range -> repository.observeMonth(range).map { HistoryState(range, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ignore(txn: TransactionEntity) {
        viewModelScope.launch { categorize.ignore(txn.id) }
    }

    fun undoIgnore(txn: TransactionEntity) {
        viewModelScope.launch { repository.unignore(txn) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                HistoryViewModel(c.transactionRepository, c.categorize)
            }
        }
    }
}
