package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.MonthlySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMonthlySummary(private val repository: TransactionRepository) {

    operator fun invoke(range: MonthRange): Flow<MonthlySummary> =
        combine(repository.observeMonth(range), repository.observeBudgets()) { transactions, budgets ->
            MonthlySummary.from(range, transactions, budgets)
        }
}
