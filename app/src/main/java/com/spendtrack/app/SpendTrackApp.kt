package com.spendtrack.app

import android.app.Application
import android.content.Context
import com.spendtrack.app.system.NotificationChannels
import com.spendtrack.app.system.PendingReminderWorker

class SpendTrackApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.create(this)
        PendingReminderWorker.schedule(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SpendTrackApp).container
