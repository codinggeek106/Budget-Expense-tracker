package com.spendtrack.app.data

import com.spendtrack.app.data.db.BudgetDao
import com.spendtrack.app.data.db.BudgetEntity
import com.spendtrack.app.data.db.CategoryTotal
import com.spendtrack.app.data.db.TransactionDao
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
) {

    suspend fun insert(transaction: TransactionEntity): Long = transactionDao.insert(transaction)

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    /** Rows whose timestamp is within [windowMillis] of [timestamp], either side. */
    suspend fun transactionsAround(timestamp: Long, windowMillis: Long): List<TransactionEntity> =
        transactionDao.getInTimeWindow(timestamp - windowMillis, timestamp + windowMillis)

    fun observeMonth(range: MonthRange): Flow<List<TransactionEntity>> =
        transactionDao.observeInRange(range.startMillis, range.endMillis)

    fun observeCategoryTotals(range: MonthRange): Flow<List<CategoryTotal>> =
        transactionDao.observeCategoryTotals(range.startMillis, range.endMillis)

    fun observeMonthTotal(range: MonthRange): Flow<Long> =
        transactionDao.observeTotal(range.startMillis, range.endMillis)

    fun observePending(): Flow<List<TransactionEntity>> = transactionDao.observePending()

    fun observePendingCount(): Flow<Int> = transactionDao.observePendingCount()

    /** Returns false if the row no longer exists. */
    suspend fun categorize(id: Long, category: String, note: String? = null): Boolean =
        transactionDao.categorize(id, category, note) > 0

    suspend fun setStatus(id: Long, status: TxnStatus): Boolean = transactionDao.setStatus(id, status) > 0

    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    suspend fun setBudget(category: String, monthlyLimitPaise: Long) =
        budgetDao.upsert(BudgetEntity(category, monthlyLimitPaise))

    suspend fun deleteBudget(category: String) = budgetDao.delete(category)
}
