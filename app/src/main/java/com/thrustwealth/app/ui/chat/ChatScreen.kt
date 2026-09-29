package com.thrustwealth.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salesmartly.chatwidget.core.ChatView
import com.salesmartly.chatwidget.ui.SalesmartlyChatHost
import com.thrustwealth.app.BuildConfig
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import com.thrustwealth.app.ui.theme.ThrustTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val salesmartlyManager: SalesmartlyManager,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val scriptConfigured: Boolean
        get() = BuildConfig.SALESMARTLY_SCRIPT_URL.isNotBlank()

    init {
        if (scriptConfigured) {
            viewModelScope.launch {
                val preferences = preferencesRepository.preferences.first()
                if (!preferences.privacyConsentAccepted) {
                    preferencesRepository.acceptPrivacyConsent()
                }
                val userId = preferences.guestUserId.takeIf { it.isNotBlank() }
                    ?: preferencesRepository.ensureGuestUserId()
                salesmartlyManager.setLoginInfo(
                    userId = userId,
                    userName = "Thrust Guest",
                    language = "en-US",
                    description = "Thrust Wealth App User",
                    labelNames = SalesmartlyManager.DEFAULT_LABELS,
                )
                salesmartlyManager.sdkReady.first { it }
                salesmartlyManager.openSupportHome()
            }
        }
    }
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val colors = ThrustTheme.colors
    val sdkState by viewModel.salesmartlyManager.uiState.collectAsState()
    var hasOpened by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    fun leaveSupport() {
        if (!leaving) {
            leaving = true
            viewModel.salesmartlyManager.closeChat()
            onBack()
        }
    }

    LaunchedEffect(sdkState?.isChatOpen) {
        if (sdkState?.isChatOpen == true) {
            hasOpened = true
        } else if (hasOpened) {
            // The SDK's close button dismisses its host; return to the app as well.
            leaveSupport()
        }
    }

    BackHandler {
        if (sdkState?.currentView == ChatView.Chat &&
            sdkState?.widgetInfo?.home_page?.enabled == true
        ) {
            viewModel.salesmartlyManager.goBackToHome()
        } else {
            leaveSupport()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF7EB))
            .navigationBarsPadding()
            .imePadding()
    ) {
        when {
            !viewModel.scriptConfigured -> {
                PlaceholderMessage(
                    title = "Support not configured",
                    body = "SaleSmartly script URL is missing. Add salesmartly.scriptUrl to local.properties and rebuild."
                )
            }
            sdkState?.isChatOpen == true -> {
                SalesmartlyChatHost(
                    runtime = viewModel.salesmartlyManager.runtime(),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = colors.brand)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Connecting to support…", color = colors.textSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderMessage(title: String, body: String) {
    val colors = ThrustTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            body,
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
