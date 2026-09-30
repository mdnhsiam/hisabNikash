package com.hisabnikash.app

import android.app.Application

class HisabApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
