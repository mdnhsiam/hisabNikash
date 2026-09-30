package com.hisabnikash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hisabnikash.app.navigation.HisabNavHost
import com.hisabnikash.app.ui.theme.HisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabTheme {
                HisabNavHost()
            }
        }
    }
}
