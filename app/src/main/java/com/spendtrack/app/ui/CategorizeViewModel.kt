package com.spendtrack.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.usecase.Categorize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TxnLoad {
    data object Loading : TxnLoad
    data object Missing : TxnLoad
    data class Loaded(val txn: TransactionEntity) : TxnLoad
}

class CategorizeViewModel(
    private val txnId: Long,
    private val repository: TransactionRepository,
    private val categorize: Categorize,
) : ViewModel() {

    private val _txn = MutableStateFlow<TxnLoad>(TxnLoad.Loading)
    val txn: StateFlow<TxnLoad> = _txn.asStateFlow()

    val choices: StateFlow<List<String>> = repository.observeUsedCategories()
        .map(Category::allChoices)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Category.allChoices(emptyList()))

    init {
        viewModelScope.launch {
            _txn.value = repository.getById(txnId)?.let(TxnLoad::Loaded) ?: TxnLoad.Missing
        }
    }

    fun save(category: String, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            categorize(txnId, category, note)
            onDone()
        }
    }

    fun ignore(onDone: () -> Unit) {
        viewModelScope.launch {
            categorize.ignore(txnId)
            onDone()
        }
    }

    companion object {
        fun factory(txnId: Long) = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                CategorizeViewModel(txnId, container.transactionRepository, container.categorize)
            }
        }
    }
}
