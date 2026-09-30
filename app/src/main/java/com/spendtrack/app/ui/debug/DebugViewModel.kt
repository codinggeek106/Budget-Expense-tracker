package com.spendtrack.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.db.RawNotificationDao
import com.spendtrack.app.data.db.RawNotificationEntity
import com.spendtrack.app.domain.model.ParsedPayment
import com.spendtrack.app.domain.usecase.RecordPayment
import com.spendtrack.app.system.PromptNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DebugViewModel(
    private val dao: RawNotificationDao,
    private val recordPayment: RecordPayment,
    private val promptNotifier: PromptNotifier,
) : ViewModel() {

    val rows: StateFlow<List<RawNotificationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }

    /** Stores a fake PENDING payment and prompts for it, to test prompting before parsers exist. */
    fun createTestPayment() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val label = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val result = recordPayment(
                ParsedPayment(
                    amountPaise = Random.nextLong(1_00, 999_00),
                    payee = "Test payment $label",
                    timestamp = now,
                    appPkg = TEST_PKG,
                    rawText = "[debug test payment] $now",
                )
            )
            if (result is RecordPayment.Result.Recorded) promptNotifier.prompt(result.id)
        }
    }

    companion object {
        const val TEST_PKG = "debug.test"

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                DebugViewModel(container.rawNotificationDao, container.recordPayment, container.promptNotifier)
            }
        }
    }
}
