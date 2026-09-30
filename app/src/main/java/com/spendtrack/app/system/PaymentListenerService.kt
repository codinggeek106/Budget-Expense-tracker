package com.spendtrack.app.system

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.spendtrack.app.appContainer
import com.spendtrack.app.data.db.RawNotificationEntity
import com.spendtrack.app.domain.UpiApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        _connected.value = true
    }

    override fun onListenerDisconnected() {
        _connected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName || pkg !in UpiApps.PACKAGES) return

        // Extract on the callback thread (cheap), then hand off; never block the callback.
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString("\n")
                ?.takeIf { it.isNotBlank() }
        val postedAt = sbn.postTime

        val container = appContainer
        container.applicationScope.launch {
            container.rawNotificationDao.insertAndTrim(
                RawNotificationEntity(
                    pkg = pkg,
                    title = title,
                    text = text,
                    bigText = bigText,
                    postedAt = postedAt,
                )
            )
        }
    }

    companion object {
        private val _connected = MutableStateFlow(false)

        /** True while the system has this listener bound in the current process. */
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        fun componentName(context: Context) = ComponentName(context, PaymentListenerService::class.java)

        /** Asks the system to re-bind the listener, e.g. after HyperOS killed the process. */
        fun requestRebind(context: Context) {
            requestRebind(componentName(context))
        }
    }
}
