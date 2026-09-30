package com.spendtrack.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.db.RawNotificationDao
import com.spendtrack.app.data.db.RawNotificationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebugViewModel(private val dao: RawNotificationDao) : ViewModel() {

    val rows: StateFlow<List<RawNotificationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DebugViewModel(this[APPLICATION_KEY]!!.appContainer.rawNotificationDao) }
        }
    }
}
