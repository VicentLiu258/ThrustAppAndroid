package com.thrustwealth.app.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.first
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

    val conversationOpen = salesmartlyManager.conversationOpen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scriptConfigured: Boolean
        get() = BuildConfig.SALESMARTLY_SCRIPT_URL.isNotBlank()

    fun enterConversation() {
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
            salesmartlyManager.openConversation()
        }
    }

    fun backToChannels() = salesmartlyManager.closeChat()

    fun externalChannelUrl(channel: String): String? {
        val info = salesmartlyManager.runtime().state.value.widgetInfo
        if (channel !in info.channel) return null
        val rawUrl = when (channel) {
            "whatsapp" -> info.show_whatsapp_config.redirect_url
            "telegram" -> info.show_telegram_config.redirect_url
            else -> return null
        }
        val uri = Uri.parse(rawUrl)
        if (uri.scheme != "https") return null
        if (channel == "whatsapp") {
            val greeting = info.show_whatsapp_config.prolusion
            if (greeting.isNotBlank()) {
                return uri.buildUpon().appendQueryParameter("text", greeting).build().toString()
            }
        }
        return rawUrl
    }

    fun runtime() = salesmartlyManager.runtime()
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val sdkReady by viewModel.sdkReady.collectAsState()
    val conversationOpen by viewModel.conversationOpen.collectAsState()
    val sdkState by viewModel.runtime().state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        AppTopBar(
            title = "Customer Support",
            onBack = if (conversationOpen) viewModel::backToChannels else onBack
        )
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
                conversationOpen -> {
                    SalesmartlyChatHost(
                        runtime = viewModel.runtime(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    SupportChannelChoice(
                        name = "thrustwealth",
                        channels = sdkState.widgetInfo.channel.filter {
                            it == "chat" || it == "whatsapp" || it == "telegram"
                        },
                        onChat = viewModel::enterConversation,
                        onExternal = { channel ->
                            viewModel.externalChannelUrl(channel)?.let { url ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportChannelChoice(
    name: String,
    channels: List<String>,
    onChat: () -> Unit,
    onExternal: (String) -> Unit
) {
    val gold = Color(0xFFE9AE17)
    val ink = Color(0xFF302C24)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBF5E8))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(name, color = ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(42.dp))
        Text("👋 Welcome!", color = ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("Please message us now", color = ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))

        if ("chat" in channels) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Text(name, color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Enter chat", color = ink, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        channels.filter { it != "chat" }.forEach { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .clickable { onExternal(channel) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (channel == "whatsapp") Color(0xFF35C759) else Color(0xFF2AA7D7),
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (channel == "whatsapp") "W" else "T", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(14.dp))
                Text(
                    if (channel == "whatsapp") "WhatsApp" else "Telegram",
                    color = ink,
                    fontSize = 16.sp
                )
                Spacer(Modifier.weight(1f))
                Text("›", color = Color.Gray, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
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
