package com.thrustwealth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.thrustwealth.app.appsflyer.AppsFlyerManager
import com.thrustwealth.app.data.model.ThemeMode
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.navigation.AppNavigation
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import com.thrustwealth.app.ui.settings.SettingsViewModel
import com.thrustwealth.app.ui.theme.ThrustWealthTheme
import com.thrustwealth.app.ui.webview.FullScreenWebView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var salesmartlyManager: SalesmartlyManager

    @Inject
    lateinit var appsFlyerManager: AppsFlyerManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fastCachedWebUrl = preferencesRepository.getSavedWebUrlSync()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.preferences.collectAsState()
            val activeWebUrl = preferences.savedWebUrl.ifBlank { fastCachedWebUrl }

            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember(preferences.theme, systemDark) {
                when (preferences.theme) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.AUTO -> systemDark
                }
            }

            // Initialize attribution and support chat for native mode.
            LaunchedEffect(preferences.guestUserId, activeWebUrl) {
                if (activeWebUrl.isNotBlank()) return@LaunchedEffect
                val userId = if (preferences.guestUserId.isBlank()) {
                    settingsViewModel.ensureGuestUserId()
                } else {
                    preferences.guestUserId
                }
                if (userId.isNotBlank()) {
                    appsFlyerManager.start(userId)
                    salesmartlyManager.initializeIfNeeded()
                    salesmartlyManager.setLoginInfo(
                        userId = userId,
                        userName = "Thrust Guest",
                        language = "en-US",
                        description = "Thrust Wealth App User",
                        labelNames = SalesmartlyManager.DEFAULT_LABELS,
                    )
                }
            }

            ThrustWealthTheme(darkTheme = darkTheme) {
                if (activeWebUrl.isNotBlank()) {
                    FullScreenWebView(url = activeWebUrl)
                } else {
                    AppNavigation()
                }
            }
        }
    }
}
