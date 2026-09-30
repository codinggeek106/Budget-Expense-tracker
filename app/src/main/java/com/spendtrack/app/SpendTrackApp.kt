package com.spendtrack.app

import android.app.Application
import android.content.Context

class SpendTrackApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SpendTrackApp).container
