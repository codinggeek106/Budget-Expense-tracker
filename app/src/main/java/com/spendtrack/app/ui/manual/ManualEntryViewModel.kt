package com.spendtrack.app.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.usecase.AddManualTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ManualEntryViewModel(
    repository: TransactionRepository,
    private val addManualTransaction: AddManualTransaction,
) : ViewModel() {

    val choices: StateFlow<List<String>> = repository.observeUsedCategories()
        .map(Category::allChoices)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Category.allChoices(emptyList()))

    fun save(amountPaise: Long, payee: String, category: String, note: String, date: LocalDate, onDone: () -> Unit) {
        viewModelScope.launch {
            addManualTransaction(amountPaise, payee, category, note, date)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                ManualEntryViewModel(c.transactionRepository, c.addManualTransaction)
            }
        }
    }
}
