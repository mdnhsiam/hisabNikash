package com.hisabnikash.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hisabnikash.app.navigation.HisabNavHost
import com.hisabnikash.app.ui.theme.HisabTheme
import com.hisabnikash.app.data.preferences.UserPreferences
import com.hisabnikash.app.feature.settings.LanguageController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking { UserPreferences.create(newBase).state.first().language }
        super.attachBaseContext(LanguageController.localizedContext(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabTheme {
                HisabNavHost(container = (application as HisabApplication).container)
            }
        }
    }
}
