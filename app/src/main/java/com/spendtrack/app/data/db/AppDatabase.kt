package com.spendtrack.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, RawNotificationEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v1 (Phase 1) only had raw_notifications; v2 adds transactions and budgets.
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun rawNotificationDao(): RawNotificationDao

    companion object {
        const val NAME = "spendtrack.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
