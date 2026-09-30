package com.spendtrack.app

import android.content.Context
import android.util.Log
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.RawNotificationDao
import com.spendtrack.app.domain.parser.ParserRegistry
import com.spendtrack.app.domain.usecase.RecordPayment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency graph. One instance per process, owned by [SpendTrackApp]. */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    /**
     * Outlives any single component; used for fire-and-forget work such as ingesting notifications.
     * Failures are logged instead of crashing the process that hosts the notification listener.
     */
    val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> Log.e(TAG, "Background work failed", e) }
    )

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val rawNotificationDao: RawNotificationDao get() = database.rawNotificationDao()

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.budgetDao())
    }

    val parserRegistry: ParserRegistry = ParserRegistry.default()

    val recordPayment: RecordPayment by lazy { RecordPayment(transactionRepository) }

    private companion object {
        const val TAG = "SpendTrack"
    }
}
