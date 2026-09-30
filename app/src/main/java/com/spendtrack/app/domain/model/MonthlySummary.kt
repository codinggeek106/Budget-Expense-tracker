package com.spendtrack.app.domain.model

import com.spendtrack.app.data.db.BudgetEntity
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.Deduper

/** Spend in one category. [category] is null for payments still waiting for a category. */
data class CategorySpend(val category: String?, val totalPaise: Long, val count: Int)

data class PayeeSpend(val payee: String, val totalPaise: Long, val count: Int)

data class BudgetProgress(val category: String, val limitPaise: Long, val spentPaise: Long) {
    /** spent / limit; above 1 when over budget. */
    val fraction: Float get() = if (limitPaise <= 0) 0f else spentPaise.toFloat() / limitPaise
    val isOver: Boolean get() = spentPaise > limitPaise
    /** Negative when over budget. */
    val remainingPaise: Long get() = limitPaise - spentPaise
}

/**
 * Everything the dashboard shows for one month. Every figure is derived from the same list of
 * that month's transactions, with IGNORED ones left out, so the parts always add up to [totalPaise].
 */
data class MonthlySummary(
    val range: MonthRange,
    val totalPaise: Long,
    val paymentCount: Int,
    val pendingPaise: Long,
    val pendingCount: Int,
    val categories: List<CategorySpend>,
    val topPayees: List<PayeeSpend>,
    val budgets: List<BudgetProgress>,
) {
    companion object {
        const val TOP_PAYEES = 5

        fun from(
            range: MonthRange,
            transactions: List<TransactionEntity>,
            budgets: List<BudgetEntity>,
        ): MonthlySummary {
            val counted = transactions.filter { it.status != TxnStatus.IGNORED && it.timestamp in range }
            val pending = counted.filter { it.status == TxnStatus.PENDING }

            val categories = counted
                .groupBy { if (it.status == TxnStatus.CATEGORIZED) it.category else null }
                .map { (category, rows) -> CategorySpend(category, rows.sumOf { it.amountPaise }, rows.size) }
                .sortedWith(compareByDescending<CategorySpend> { it.totalPaise }.thenBy { it.category ?: "￿" })

            val topPayees = counted
                .groupBy { Deduper.normalizePayee(it.payee) }
                .map { (_, rows) ->
                    // Show the spelling used most often for this payee.
                    val name = rows.groupingBy { it.payee.trim() }.eachCount().maxBy { it.value }.key
                    PayeeSpend(name, rows.sumOf { it.amountPaise }, rows.size)
                }
                .sortedWith(compareByDescending<PayeeSpend> { it.totalPaise }.thenBy { it.payee.lowercase() })
                .take(TOP_PAYEES)

            val budgetProgress = budgets
                .map { budget ->
                    val spent = counted
                        .filter { it.status == TxnStatus.CATEGORIZED && it.category.equals(budget.category, ignoreCase = true) }
                        .sumOf { it.amountPaise }
                    BudgetProgress(budget.category, budget.monthlyLimitPaise, spent)
                }
                .sortedBy { it.category.lowercase() }

            return MonthlySummary(
                range = range,
                totalPaise = counted.sumOf { it.amountPaise },
                paymentCount = counted.size,
                pendingPaise = pending.sumOf { it.amountPaise },
                pendingCount = pending.size,
                categories = categories,
                topPayees = topPayees,
                budgets = budgetProgress,
            )
        }
    }
}
