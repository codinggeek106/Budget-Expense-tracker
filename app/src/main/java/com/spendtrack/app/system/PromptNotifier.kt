package com.spendtrack.app.system

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.spendtrack.app.R
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.model.TxnStatus
import com.spendtrack.app.ui.CategorizeActivity

/**
 * Heads-up "what was this for?" prompt for a PENDING transaction. Everything the actions need
 * travels in the PendingIntents, so they work after process death.
 */
class PromptNotifier(
    private val context: Context,
    private val repository: TransactionRepository,
) {

    private val manager = NotificationManagerCompat.from(context)

    /** Prompts for [txnId] if it is still PENDING. */
    suspend fun prompt(txnId: Long) {
        val txn = repository.getById(txnId)?.takeIf { it.status == TxnStatus.PENDING } ?: return
        promptAll(listOf(txn))
    }

    suspend fun promptAll(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        val top = repository.getTopCategories(ACTION_COUNT)
        transactions.forEach { txn ->
            val suggestion = repository.getLastCategoryForPayee(txn.payee)
            show(txn, Category.promptChoices(listOfNotNull(suggestion) + top), suggestion)
        }
    }

    /**
     * Posts (or re-posts) the prompt. [suggestion] is the category last used for this payee; it
     * should already be the first of [choices]. Returns false if notifications aren't allowed.
     */
    fun show(txn: TransactionEntity, choices: List<String>, suggestion: String? = null): Boolean {
        if (!canPost()) return false

        val amount = Money.format(txn.amountPaise)
        val openPicker = PendingIntent.getActivity(
            context,
            requestCode(txn.id, SLOT_CONTENT),
            CategorizeActivity.intent(context, txn.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.PAYMENT_PROMPT)
            .setSmallIcon(R.drawable.ic_stat_rupee)
            .setContentTitle("Paid $amount to ${txn.payee}")
            .setContentText(if (suggestion != null) "What was this for? Last time: $suggestion" else "What was this for?")
            .setSubText(UpiApps.label(txn.appPkg))
            .setWhen(txn.timestamp)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .setContentIntent(openPicker)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, NotificationChannels.PAYMENT_PROMPT)
                    .setSmallIcon(R.drawable.ic_stat_rupee)
                    .setContentTitle("New payment to categorize")
                    .build()
            )

        choices.take(ACTION_COUNT).forEachIndexed { index, category ->
            builder.addAction(
                NotificationCompat.Action.Builder(
                    null as IconCompat?,
                    category,
                    categoryPendingIntent(txn.id, index, category),
                ).build()
            )
        }

        if (canUseFullScreenIntent()) {
            val fullScreen = PendingIntent.getActivity(
                context,
                requestCode(txn.id, SLOT_FULL_SCREEN),
                CategorizeActivity.intent(context, txn.id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.setFullScreenIntent(fullScreen, true)
        }

        return try {
            manager.notify(TAG, notificationId(txn.id), builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(txnId: Long) {
        manager.cancel(TAG, notificationId(txnId))
    }

    private fun categoryPendingIntent(txnId: Long, slot: Int, category: String): PendingIntent {
        val intent = Intent(context, CategoryActionReceiver::class.java)
            .setAction(CategoryActionReceiver.ACTION_CATEGORIZE)
            // Unique data keeps each (txn, action) PendingIntent distinct even if request codes collide.
            .setData(Uri.parse("spendtrack://txn/$txnId/action/$slot"))
            .putExtra(CategoryActionReceiver.EXTRA_TXN_ID, txnId)
            .putExtra(CategoryActionReceiver.EXTRA_CATEGORY, category)
        return PendingIntent.getBroadcast(
            context,
            requestCode(txnId, slot),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    companion object {
        const val TAG = "payment_prompt"
        const val ACTION_COUNT = 3

        private const val SLOT_CONTENT = 5
        private const val SLOT_FULL_SCREEN = 6
        private const val SLOTS_PER_TXN = 8

        fun notificationId(txnId: Long): Int = txnId.toInt()

        /** Distinct per (txnId, slot) for any realistic txnId (< 268M rows). */
        fun requestCode(txnId: Long, slot: Int): Int = (txnId * SLOTS_PER_TXN + slot).toInt()
    }
}
