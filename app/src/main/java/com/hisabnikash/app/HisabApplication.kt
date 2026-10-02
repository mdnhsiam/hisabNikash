package com.hisabnikash.app

import android.app.Application
import com.hisabnikash.app.feature.recurring.NotificationFactory

class HisabApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        NotificationFactory(this).createChannel()
    }
}
