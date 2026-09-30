package com.spendtrack.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.usecase.GetMonthlySummary
import com.spendtrack.app.domain.usecase.SetBudget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One category on the budget screen, with this month's spend for context. */
data class BudgetRowState(val category: String, val limitPaise: Long?, val spentThisMonthPaise: Long)

class BudgetViewModel(
    repository: TransactionRepository,
    getMonthlySummary: GetMonthlySummary,
    private val setBudget: SetBudget,
) : ViewModel() {

    val rows: StateFlow<List<BudgetRowState>?> = combine(
        repository.observeUsedCategories(),
        repository.observeBudgets(),
        getMonthlySummary(MonthRange.current()),
    ) { used, budgets, summary ->
        val limits = budgets.associate { it.category.lowercase() to it.monthlyLimitPaise }
        val spent = summary.categories.filter { it.category != null }
            .associate { it.category!!.lowercase() to it.totalPaise }
        Category.allChoices(used + budgets.map { it.category }).map { category ->
            BudgetRowState(category, limits[category.lowercase()], spent[category.lowercase()] ?: 0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(category: String, limitPaise: Long?) {
        viewModelScope.launch { setBudget(category, limitPaise) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                BudgetViewModel(c.transactionRepository, c.getMonthlySummary, c.setBudget)
            }
        }
    }
}
