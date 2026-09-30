package com.spendtrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.domain.model.MonthlySummary
import com.spendtrack.app.domain.usecase.GetMonthlySummary
import com.spendtrack.app.ui.components.MonthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(getMonthlySummary: GetMonthlySummary) : ViewModel() {

    val month = MonthState()

    /** null while the selected month is loading. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<MonthlySummary?> = month.range
        .flatMapLatest { getMonthlySummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { StatsViewModel(this[APPLICATION_KEY]!!.appContainer.getMonthlySummary) }
        }
    }
}
