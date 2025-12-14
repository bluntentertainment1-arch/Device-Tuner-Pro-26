package com.kidblunt.cleanerguru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kidblunt.cleanerguru.ui.navigation.AppNavigation
import com.kidblunt.cleanerguru.ui.theme.CleanerGuruTheme
import android.content.Context
import android.content.SharedPreferences
import com.dexati.analytics.UserApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var sharedPreferences: SharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        var userId = sharedPreferences.getString("user_id", "")
        UserApi.logUserAnalytics("5380bb5c-c23c-4a7e-acbf-07ee7a412b56",userId!!, applicationContext,"https://api.lastapp.ai/")
        
        installSplashScreen()
        
        setContent {
            CleanerGuruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}