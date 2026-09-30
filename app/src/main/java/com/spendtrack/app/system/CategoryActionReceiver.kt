package com.spendtrack.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spendtrack.app.appContainer
import kotlinx.coroutines.launch

/** Handles a category button on the payment prompt. All inputs come from the Intent extras. */
class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CATEGORIZE) return
        val txnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)
        val category = intent.getStringExtra(EXTRA_CATEGORY)
        if (txnId < 0 || category.isNullOrBlank()) return

        val container = context.appContainer
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                container.categorize(txnId, category)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CATEGORIZE = "com.spendtrack.app.action.CATEGORIZE"
        const val EXTRA_TXN_ID = "txnId"
        const val EXTRA_CATEGORY = "category"
    }
}
