package com.hisabnikash.app

import android.content.Context
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hisabnikash.app.navigation.HisabNavHost
import com.hisabnikash.app.ui.theme.HisabTheme
import com.hisabnikash.app.data.preferences.UserPreferences
import com.hisabnikash.app.feature.settings.LanguageController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val requestedRecurringId = mutableStateOf<String?>(null)
    private val requestedRecurringDueAt = mutableStateOf<Long?>(null)
    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking { UserPreferences.create(newBase).state.first().language }
        super.attachBaseContext(LanguageController.localizedContext(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRecurringId.value = intent.getStringExtra(com.hisabnikash.app.feature.recurring.NotificationFactory.EXTRA_RECURRING_ID)
        requestedRecurringDueAt.value = intent.getLongExtra(com.hisabnikash.app.feature.recurring.NotificationFactory.EXTRA_DUE_AT, Long.MIN_VALUE)
        enableEdgeToEdge()
        setContent {
            HisabTheme {
                HisabNavHost(
                    container = (application as HisabApplication).container,
                    requestedRecurringId = requestedRecurringId.value,
                    requestedRecurringDueAt = requestedRecurringDueAt.value,
                    onRecurringRequestConsumed = { requestedRecurringId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRecurringId.value = intent.getStringExtra(com.hisabnikash.app.feature.recurring.NotificationFactory.EXTRA_RECURRING_ID)
        requestedRecurringDueAt.value = intent.getLongExtra(com.hisabnikash.app.feature.recurring.NotificationFactory.EXTRA_DUE_AT, Long.MIN_VALUE)
    }
}
