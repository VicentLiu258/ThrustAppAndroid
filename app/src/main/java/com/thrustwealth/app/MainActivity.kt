package com.thrustwealth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.thrustwealth.app.data.model.ThemeMode
import com.thrustwealth.app.navigation.AppNavigation
import com.thrustwealth.app.ui.settings.SettingsViewModel
import com.thrustwealth.app.ui.theme.ThrustWealthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember(preferences.theme, systemDark) {
                when (preferences.theme) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.AUTO -> systemDark
                }
            }
            ThrustWealthTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
}
