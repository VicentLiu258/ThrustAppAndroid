package com.thrustwealth.app.ui.chat

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salesmartly.chatwidget.ui.SalesmartlyChatHost
import com.thrustwealth.app.BuildConfig
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.theme.ThrustTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val salesmartlyManager: SalesmartlyManager,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val sdkReady = salesmartlyManager.sdkReady
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scriptConfigured: Boolean
        get() = BuildConfig.SALESMARTLY_SCRIPT_URL.isNotBlank()

    fun ensureIdentityAndOpen() {
        viewModelScope.launch {
            if (preferences.value?.privacyConsentAccepted != true) {
                preferencesRepository.acceptPrivacyConsent()
            }
            val userId = preferences.value?.guestUserId?.takeIf { it.isNotBlank() }
                ?: preferencesRepository.ensureGuestUserId()
            salesmartlyManager.setLoginInfo(
                userId = userId,
                userName = "Thrust Guest",
                language = "en-US",
                description = "Thrust Wealth App User",
                labelNames = SalesmartlyManager.DEFAULT_LABELS,
            )
            salesmartlyManager.openChat()
        }
    }

    fun runtime() = salesmartlyManager.runtime()
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val colors = ThrustTheme.colors
    val sdkReady by viewModel.sdkReady.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    var openedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(sdkReady, prefs?.guestUserId) {
        if (!openedOnce && sdkReady && prefs != null) {
            openedOnce = true
            viewModel.ensureIdentityAndOpen()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        AppTopBar(title = "Customer Support", onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            when {
                !viewModel.scriptConfigured -> {
                    PlaceholderMessage(
                        title = "Support not configured",
                        body = "SaleSmartly script URL is missing. Add salesmartly.scriptUrl to local.properties and rebuild."
                    )
                }
                !sdkReady -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = colors.brand)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Connecting to support…", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    SalesmartlyChatHost(
                        runtime = viewModel.runtime(),
                        modifier = Modifier.fillMaxSize()
                    )
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
