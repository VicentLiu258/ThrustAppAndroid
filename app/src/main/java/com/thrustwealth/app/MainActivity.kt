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
import com.thrustwealth.app.data.model.ThemeMode
import com.thrustwealth.app.navigation.AppNavigation
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import com.thrustwealth.app.ui.components.PrivacyConsentDialog
import com.thrustwealth.app.ui.settings.SettingsViewModel
import com.thrustwealth.app.ui.theme.ThrustWealthTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var salesmartlyManager: SalesmartlyManager

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

            // Initialize SaleSmartly only after privacy consent (compliance requirement).
            LaunchedEffect(preferences.privacyConsentAccepted) {
                if (preferences.privacyConsentAccepted) {
                    salesmartlyManager.initializeIfNeeded()
                    val userId = preferences.guestUserId
                    if (userId.isNotBlank()) {
                        salesmartlyManager.setLoginInfo(
                            userId = userId,
                            userName = "Thrust Guest",
                            language = "en-US",
                            description = "Thrust Wealth App User",
                            labelNames = SalesmartlyManager.DEFAULT_LABELS,
                        )
                    }
                }
            }

            ThrustWealthTheme(darkTheme = darkTheme) {
                if (!preferences.privacyConsentAccepted) {
                    PrivacyConsentDialog(
                        onAccept = { settingsViewModel.acceptPrivacyConsent() },
                        onDecline = { finish() }
                    )
                }
                AppNavigation()
            }
        }
    }
}
