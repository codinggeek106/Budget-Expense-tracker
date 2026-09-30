package com.spendtrack.app.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spendtrack.app.appContainer
import java.util.concurrent.TimeUnit

/** Re-posts prompts for payments that are still PENDING some time after they were made. */
class PendingReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val minAge = inputData.getLong(KEY_MIN_AGE_MS, DEFAULT_MIN_AGE_MS)
        val stale = container.transactionRepository.getPendingOlderThan(System.currentTimeMillis() - minAge)
        container.promptNotifier.promptAll(stale)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "pending_reminder"
        const val KEY_MIN_AGE_MS = "minAgeMs"
        const val DEFAULT_MIN_AGE_MS = 10 * 60 * 1000L
        private const val INTERVAL_MINUTES = 30L

        /** Idempotent: keeps the existing schedule if there is one. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PendingReminderWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Debug helper: re-post prompts for every PENDING row right now, regardless of age. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<PendingReminderWorker>()
                .setInputData(workDataOf(KEY_MIN_AGE_MS to 0L))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
