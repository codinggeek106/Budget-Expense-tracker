package com.spendtrack.app.domain.usecase

import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.domain.model.Category

class SetBudget(private val repository: TransactionRepository) {

    /** Sets the monthly limit for [category]; a null or non-positive limit removes the budget. */
    suspend operator fun invoke(category: String, monthlyLimitPaise: Long?) {
        if (category.isBlank()) return
        val canonical = Category.canonical(category, repository.getUsedCategories())
        if (monthlyLimitPaise == null || monthlyLimitPaise <= 0) {
            repository.deleteBudget(canonical)
        } else {
            repository.setBudget(canonical, monthlyLimitPaise)
        }
    }
}
