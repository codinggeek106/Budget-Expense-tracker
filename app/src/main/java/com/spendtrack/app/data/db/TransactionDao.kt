package com.spendtrack.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.flow.Flow

/** Spend per category. [category] is null for rows still PENDING. */
data class CategoryTotal(
    val category: String?,
    val totalPaise: Long,
    val count: Int,
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** Every row (any status) with a timestamp in the closed range [from, to]; dedupe candidates. */
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to")
    suspend fun getInTimeWindow(from: Long, to: Long): List<TransactionEntity>

    /** All rows (any status) in [start, end), newest first. */
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC, id DESC")
    fun observeInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    /** Non-ignored spend in [start, end), largest category first. */
    @Query(
        "SELECT category, SUM(amountPaise) AS totalPaise, COUNT(*) AS count FROM transactions " +
            "WHERE timestamp >= :start AND timestamp < :end AND status != 'IGNORED' " +
            "GROUP BY category ORDER BY totalPaise DESC"
    )
    fun observeCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    /** Total non-ignored spend in [start, end); 0 when there is none. */
    @Query(
        "SELECT COALESCE(SUM(amountPaise), 0) FROM transactions " +
            "WHERE timestamp >= :start AND timestamp < :end AND status != 'IGNORED'"
    )
    fun observeTotal(start: Long, end: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC, id DESC")
    fun observePending(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    /** PENDING rows paid at or before [cutoff], oldest first. */
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND timestamp <= :cutoff ORDER BY timestamp, id")
    suspend fun getPendingOlderThan(cutoff: Long): List<TransactionEntity>

    /** Most frequently chosen categories, ties broken by most recent use. */
    @Query(
        "SELECT category FROM transactions WHERE status = 'CATEGORIZED' AND category IS NOT NULL " +
            "GROUP BY category ORDER BY COUNT(*) DESC, MAX(timestamp) DESC LIMIT :limit"
    )
    suspend fun getTopCategories(limit: Int): List<String>

    @Query("SELECT DISTINCT category FROM transactions WHERE category IS NOT NULL")
    suspend fun getUsedCategories(): List<String>

    /**
     * The category most recently chosen for [normalizedPayee] (trimmed, lowercased; see
     * Deduper.normalizePayee). SQLite's LOWER only folds ASCII, which covers UPI payee names.
     */
    @Query(
        "SELECT category FROM transactions WHERE status = 'CATEGORIZED' AND category IS NOT NULL " +
            "AND LOWER(TRIM(payee)) = :normalizedPayee ORDER BY timestamp DESC, id DESC LIMIT 1"
    )
    suspend fun getLastCategoryForPayee(normalizedPayee: String): String?

    /** Rows in [start, end), oldest first; for export. */
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp, id")
    suspend fun getInRange(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp, id")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT DISTINCT category FROM transactions WHERE category IS NOT NULL")
    fun observeUsedCategories(): Flow<List<String>>

    /** Sets the category (and note, if given) and marks the row CATEGORIZED. Returns rows updated. */
    @Query(
        "UPDATE transactions SET category = :category, note = COALESCE(:note, note), status = 'CATEGORIZED' " +
            "WHERE id = :id"
    )
    suspend fun categorize(id: Long, category: String, note: String? = null): Int

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: TxnStatus): Int
}
