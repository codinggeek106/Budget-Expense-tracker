package com.spendtrack.app.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val PAYMENT_PROMPT = "payment_prompt"

    /** Safe to call repeatedly; the system keeps user changes to existing channels. */
    fun create(context: Context) {
        val channel = NotificationChannel(PAYMENT_PROMPT, "Payment prompts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asks what each detected UPI payment was for"
            enableVibration(true)
            // Default notification sound is kept (not setting a sound means the default is used).
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
