package com.spendtrack.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {

    @Insert
    suspend fun insert(entity: RawNotificationEntity): Long

    /** Keeps only the [keep] most recently inserted rows. */
    @Query(
        "DELETE FROM raw_notifications WHERE id NOT IN " +
            "(SELECT id FROM raw_notifications ORDER BY id DESC LIMIT :keep)"
    )
    suspend fun trimTo(keep: Int)

    @Transaction
    suspend fun insertAndTrim(entity: RawNotificationEntity, keep: Int = MAX_ROWS): Long {
        val id = insert(entity)
        trimTo(keep)
        return id
    }

    @Query("SELECT * FROM raw_notifications ORDER BY id DESC")
    fun observeAll(): Flow<List<RawNotificationEntity>>

    @Query("SELECT COUNT(*) FROM raw_notifications")
    suspend fun count(): Int

    @Query("DELETE FROM raw_notifications")
    suspend fun clear()

    companion object {
        const val MAX_ROWS = 500
    }
}
