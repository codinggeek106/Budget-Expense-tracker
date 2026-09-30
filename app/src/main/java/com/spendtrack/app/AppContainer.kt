package com.spendtrack.app

import android.content.Context
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.RawNotificationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency graph. One instance per process, owned by [SpendTrackApp]. */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    /** Outlives any single component; used for fire-and-forget work such as ingesting notifications. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val rawNotificationDao: RawNotificationDao get() = database.rawNotificationDao()
}
