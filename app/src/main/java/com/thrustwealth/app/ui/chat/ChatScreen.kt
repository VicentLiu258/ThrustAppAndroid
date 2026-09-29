package com.thrustwealth.app.ui.chat

import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salesmartly.chatwidget.core.SalesmartlyUiState
import com.salesmartly.chatwidget.ui.SalesmartlyChatHost
import com.thrustwealth.app.BuildConfig
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import com.thrustwealth.app.ui.theme.ThrustTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val salesmartlyManager: SalesmartlyManager,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val sdkReady = salesmartlyManager.sdkReady
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

    fun backToChannels() {
        salesmartlyManager.closeChat()
    }

    val salesmartlyUiState: StateFlow<SalesmartlyUiState?> = salesmartlyManager.uiState

    fun runtime() = salesmartlyManager.runtime()
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val colors = ThrustTheme.colors
    val sdkReady by viewModel.sdkReady.collectAsState()
    val conversationOpen by viewModel.salesmartlyManager.conversationOpen.collectAsState()
    val sdkState by viewModel.salesmartlyUiState.collectAsState()

    val widgetInfo = sdkState?.widgetInfo
    val avatarUrl = widgetInfo?.avatar_url
    val brandName = remember(widgetInfo?.plugin_name, widgetInfo?.window_name) {
        widgetInfo?.plugin_name?.ifBlank { widgetInfo.window_name }?.ifBlank { "thrustwealth" } ?: "thrustwealth"
    }
    val brandColor = remember(widgetInfo?.background_color) {
        parseHexColor(widgetInfo?.background_color, fallback = Color(0xFFE9AE17))
    }
    val welcomeText = remember(widgetInfo?.welcome) {
        widgetInfo?.welcome?.trim()?.ifBlank { "👋 Welcome!\nPlease message us now" } ?: "👋 Welcome!\nPlease message us now"
    }
    val enterChatAction = {
        viewModel.enterConversation()
    }
    BackHandler(enabled = true) {
        if (conversationOpen) {
            viewModel.backToChannels()
        } else {
            onBack()
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
            conversationOpen -> {
                SalesmartlyChatHost(
                    runtime = viewModel.runtime(),
                    modifier = Modifier.fillMaxSize()
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
                NativeSupportChannelView(
                    brandName = brandName,
                    avatarUrl = avatarUrl,
                    welcomeText = welcomeText,
                    brandColor = brandColor,
                    onEnterChat = enterChatAction,
                    onClose = onBack
                )
            }
        }
    }
}

/**
 * SaleSmartly channel entry. The visitor chooses Enter chat before the SDK conversation opens.
 */
@Composable
private fun NativeSupportChannelView(
    brandName: String,
    avatarUrl: String?,
    welcomeText: String,
    brandColor: Color,
    onEnterChat: () -> Unit,
    onClose: () -> Unit
) {
    val inkText = Color(0xFF1E1E1E)

    // 背景微渐变，与官方图片一致的高质感米白淡金渐变
    val pageBgGradient = remember(brandColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF7EB),
                Color(0xFFFFF9F0),
                Color(0xFFFBF8F2)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // 顶栏：Logo 头像 + 品牌名 + 右上角关闭按钮 (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 品牌 Logo 头像（优先从后台 avatar_url 加载）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1917)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = brandName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = brandName,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = brandName,
                    color = inkText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // 右上角关闭叉号
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8C857B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 欢迎标语部分：👋Welcome! / Please message us now
            Column {
                Text(
                    text = "👋Welcome!",
                    color = inkText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val subline = remember(welcomeText) {
                    welcomeText.lines().lastOrNull { it.isNotBlank() } ?: "Please message us now"
                }
                Text(
                    text = subline,
                    color = brandColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 核心白色大卡片：包含居中品牌圆形 Logo、右上角 "Hi" 对话小气泡、以及 "Enter chat" 按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color(0x15000000))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF0EBE1), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 居中大头像与右上方 Hi 小气泡徽章
                    Box(
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF221F1F))
                                .border(1.5.dp, Color(0xFFEFE9DC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = brandName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = brandName,
                                    tint = brandColor,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // 右上方 "Hi" 气泡徽标
                        Box(
                            modifier = Modifier
                                .offset(x = 10.dp, y = (-2).dp)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 2.dp))
                                .background(brandColor)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hi",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // "Enter chat" 椭圆胶囊大按钮
                    Button(
                        onClick = onEnterChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = Color(0xFF1F1A14),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter chat",
                                color = Color(0xFF1F1A14),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 欢迎页面仅保留居中的核心对话卡片（按需移除 WhatsApp 等外部入口）
        }
    }
}

private fun parseHexColor(hex: String?, fallback: Color = Color(0xFFE9AE17)): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val clean = hex.trim()
        val formatted = if (clean.startsWith("#")) clean else "#$clean"
        Color(android.graphics.Color.parseColor(formatted))
    }.getOrDefault(fallback)
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
