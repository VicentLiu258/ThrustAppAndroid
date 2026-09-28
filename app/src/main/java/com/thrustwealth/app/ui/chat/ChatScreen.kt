package com.thrustwealth.app.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salesmartly.chatwidget.core.SalesmartlyUiState
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

    var directConversationOpen by mutableStateOf(false)
        private set

    val scriptConfigured: Boolean
        get() = BuildConfig.SALESMARTLY_SCRIPT_URL.isNotBlank() || BuildConfig.SALESMARTLY_WEB_URL.isNotBlank()

    val scriptUrl: String
        get() = BuildConfig.SALESMARTLY_SCRIPT_URL.trim()

    val directWebUrl: String
        get() = BuildConfig.SALESMARTLY_WEB_URL.trim()

    var cachedUserId by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val preferences = preferencesRepository.preferences.first()
            if (!preferences.privacyConsentAccepted) {
                preferencesRepository.acceptPrivacyConsent()
            }
            cachedUserId = preferences.guestUserId.takeIf { it.isNotBlank() }
                ?: preferencesRepository.ensureGuestUserId()
        }
    }

    fun enterConversation() {
        directConversationOpen = true
        viewModelScope.launch {
            val preferences = preferencesRepository.preferences.first()
            if (!preferences.privacyConsentAccepted) {
                preferencesRepository.acceptPrivacyConsent()
            }
            val userId = preferences.guestUserId.takeIf { it.isNotBlank() }
                ?: preferencesRepository.ensureGuestUserId()
            cachedUserId = userId
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
        directConversationOpen = false
        salesmartlyManager.closeChat()
    }

    val salesmartlyUiState: StateFlow<SalesmartlyUiState?> = salesmartlyManager.uiState

    fun externalChannelUrl(channel: String): String? = salesmartlyManager.externalChannelUrl(channel)

    fun runtime() = salesmartlyManager.runtime()

    fun runtimeOrNull() = salesmartlyManager.runtimeOrNull()
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val sdkReady by viewModel.sdkReady.collectAsState()
    val sdkConversationOpen by viewModel.salesmartlyManager.conversationOpen.collectAsState()
    val conversationOpen = viewModel.directConversationOpen || sdkConversationOpen
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
                SalesmartlyWebChatHost(
                    scriptUrl = viewModel.scriptUrl,
                    directWebUrl = viewModel.directWebUrl,
                    userId = viewModel.cachedUserId,
                    onCloseChat = { viewModel.backToChannels() },
                    modifier = Modifier.fillMaxSize(),
                    onExternalUrl = { url ->
                        handleExternalUrl(context, url)
                    }
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
 * 响应式原生客服渠道面板（完全由 SaleSmartly 后台 widgetInfo 数据流驱动渲染）。
 *
 * 1. 响应式背景色与卡片主题色：读取后台 [brandColor]；
 * 2. 欢迎语与品牌标题：读取后台 [brandName] 与 [welcomeText]；
 * 3. 渠道列表与排序：按后台 [channels] 排序显示 Chat、WhatsApp、Telegram、Line、Email 等；
 * 4. 点击“Enter chat / 在线咨询”：直接进入官方原生聊天流 [SalesmartlyChatHost]；
 * 5. 点击“WhatsApp / Telegram 等”：自动拉起原生 App 并携带预设问候语。
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
    val colors = ThrustTheme.colors
    val isDark = colors.isDark
    val inkText = Color(0xFF1E1E1E)
    val buttonTextColor = if (brandColor.luminance() > 0.55f) Color(0xFF1F1A14) else Color.White

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

/**
 * SaleSmartly 官方 Web 挂件容器。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SalesmartlyWebChatHost(
    scriptUrl: String,
    directWebUrl: String,
    userId: String,
    onCloseChat: () -> Unit,
    modifier: Modifier = Modifier,
    onExternalUrl: (String) -> Unit
) {
    var isPageLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val result = if (!uris.isNullOrEmpty()) uris.toTypedArray() else null
        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCloseChat()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.WHITE)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "$userAgentString ThrustWealth/1.0.0 Mobile"
                        setSupportMultipleWindows(true)
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onCloseChat() {
                            post { onCloseChat() }
                        }
                    }, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 70) {
                                isPageLoading = false
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            callback: ValueCallback<Array<Uri>>?,
                            params: FileChooserParams?
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            val acceptTypes = params?.acceptTypes?.filter { it.isNotBlank() }
                            val mimeType = if (!acceptTypes.isNullOrEmpty()) acceptTypes[0] else "*/*"
                            return try {
                                fileChooserLauncher.launch(mimeType)
                                true
                            } catch (e: Exception) {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = null
                                false
                            }
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            val tempWebView = WebView(view?.context ?: return false)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val targetUrl = request?.url?.toString() ?: return false
                                    onExternalUrl(targetUrl)
                                    return true
                                }
                            }
                            transport?.webView = tempWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            val uri = request.url
                            val scheme = uri.scheme?.lowercase() ?: ""

                            if (scheme == "whatsapp" || scheme == "tg" || scheme == "line" || scheme == "mailto" || scheme == "tel") {
                                onExternalUrl(url)
                                return true
                            }
                            val host = uri.host?.lowercase() ?: ""
                            if (host.contains("whatsapp.com") || host == "wa.me" || host.contains("telegram.me") || host == "t.me" || host.contains("line.me")) {
                                onExternalUrl(url)
                                return true
                            }
                            if (!host.contains("salesmartly.com")) {
                                onExternalUrl(url)
                                return true
                            }
                            return false
                        }
                    }

                    if (directWebUrl.isNotBlank()) {
                        loadUrl(directWebUrl)
                    } else {
                        val html = buildSaleSmartlyWebHtml(scriptUrl = scriptUrl, userId = userId)
                        loadDataWithBaseURL("https://app.salesmartly.com/", html, "text/html", "UTF-8", null)
                    }
                    webViewInstance = this
                }
            },
            update = { view ->
                webViewInstance = view
            }
        )

        if (isPageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThrustTheme.colors.bgApp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ThrustTheme.colors.brand)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

private fun buildSaleSmartlyWebHtml(scriptUrl: String, userId: String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
          <title>Customer Support</title>
          <style>
            * {
              box-sizing: border-box;
              margin: 0;
              padding: 0;
              -webkit-tap-highlight-color: transparent;
            }
            html, body {
              width: 100%;
              height: 100%;
              margin: 0;
              padding: 0;
              background-color: #FAF5EB;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            }
            #ss-chat-container {
              width: 100%;
              height: 100%;
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              bottom: 0;
              display: flex;
              justify-content: center;
              align-items: center;
              overflow: hidden;
            }
            #ss-chat-page {
              width: 100%;
              height: 100%;
              position: relative;
            }
            #ss-chat-page iframe {
              width: 100% !important;
              height: 100% !important;
              border: none !important;
            }
            .preview-spacer {
              display: none !important;
            }
            #ss-chat-container .salesmartly-chat-widget,
            #ss-chat-page .salesmartly-chat-widget {
              width: 100% !important;
              height: 100% !important;
            }
            iframe[id*="salesmartly"], iframe[class*="salesmartly"] {
              width: 100% !important;
              height: 100% !important;
            }
          </style>
        </head>
        <body>
          <div id="ss-chat-container">
            <section id="ss-chat-page" class="center"></section>
          </div>
          <script id="insertJs"></script>
          <script>
            window.__ssc = window.__ssc || {};
            window.__ssc.setting = {
              mode: 'preview',
              centerTarget: document.getElementById('ss-chat-page')
            };

            window.ssq = window.ssq || function() { (window.ssq.q = window.ssq.q || []).push(arguments) };
            window.ssq.push = window.ssq;

            window.ssq.push("onReady", function() {
              window.ssq.push("openChat");
              try {
                var btn = document.querySelector('.salesmartly-chat-widget, [class*="salesmartly"]');
                if (btn) btn.click();
              } catch(e) {}
            });

            window.ssq.push("onCloseChat", function() {
              if (window.AndroidBridge && window.AndroidBridge.onCloseChat) {
                window.AndroidBridge.onCloseChat();
              }
            });

            var uid = "$userId";
            if (uid && uid.length > 0) {
              window.ssq.push("setLoginInfo", {
                user_id: uid,
                user_name: "Thrust Guest",
                language: "en-US",
                description: "Thrust Wealth App User",
                label_names: ["android", "thrust_wealth"]
              });
            }

            window.ssq.push("openChat");
            setTimeout(function() {
              window.ssq.push("openChat");
              try {
                var els = document.querySelectorAll('iframe, button, div');
                for (var i = 0; i < els.length; i++) {
                  if (els[i].className && (els[i].className.indexOf('salesmartly') !== -1 || els[i].className.indexOf('chat-button') !== -1)) {
                    els[i].click();
                    break;
                  }
                }
              } catch(e) {}
            }, 800);
          </script>
          <script src="$scriptUrl"></script>
        </body>
        </html>
    """.trimIndent()
}

private fun handleExternalUrl(context: Context, url: String) {
    runCatching {
        val uri = Uri.parse(url)
        val intent = if (url.startsWith("mailto:", ignoreCase = true)) {
            Intent(Intent.ACTION_SENDTO, uri)
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        android.widget.Toast.makeText(context, "Could not open external application", android.widget.Toast.LENGTH_SHORT).show()
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
