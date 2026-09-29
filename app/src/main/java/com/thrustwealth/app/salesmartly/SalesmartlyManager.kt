package com.thrustwealth.app.salesmartly

import android.content.Context
import android.net.Uri
import android.util.Log
import com.salesmartly.chatwidget.api.LoginInfo
import com.salesmartly.chatwidget.api.SalesmartlyCallback
import com.salesmartly.chatwidget.api.SalesmartlyChat
import com.salesmartly.chatwidget.core.SalesmartlyRuntime
import com.salesmartly.chatwidget.core.SalesmartlyUiState
import com.salesmartly.chatwidget.domain.model.channelOpenUri
import com.thrustwealth.app.BuildConfig
import com.thrustwealth.app.appsflyer.AppsFlyerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SaleSmartly Chat SDK facade.
 *
 * Must only call [initializeIfNeeded] after the user has accepted the privacy policy.
 */
@Singleton
class SalesmartlyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appsFlyerManager: AppsFlyerManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _conversationOpen = MutableStateFlow(false)
    val conversationOpen: StateFlow<Boolean> = _conversationOpen.asStateFlow()

    private val _uiState = MutableStateFlow<SalesmartlyUiState?>(null)
    val uiState: StateFlow<SalesmartlyUiState?> = _uiState.asStateFlow()

    @Volatile
    private var initialized = false

    @Volatile
    private var currentUserId: String? = null

    init {
        // 如果已经有脚本配置，尽早尝试安全初始化
        val scriptUrl = BuildConfig.SALESMARTLY_SCRIPT_URL.trim()
        if (scriptUrl.isNotEmpty()) {
            initializeIfNeeded()
        }
    }

    @Synchronized
    fun initializeIfNeeded() {
        if (initialized) return
        val scriptUrl = BuildConfig.SALESMARTLY_SCRIPT_URL.trim()
        if (scriptUrl.isEmpty()) {
            Log.w(TAG, "SALESMARTLY_SCRIPT_URL is empty; skip SDK init. Set salesmartly.scriptUrl in local.properties.")
            return
        }

        SalesmartlyChat.push("onReady", SalesmartlyCallback {
            _sdkReady.value = true
            Log.d(TAG, "SaleSmartly SDK ready")
            // SDK 准备就绪后，启动协程监听 runtime 的状态流并同步给 Compose
            scope.launch {
                runCatching {
                    SalesmartlyChat.runtime().state.collect { state ->
                        _uiState.value = state
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Failed to collect runtime state in onReady", e)
                }
            }
        })

        SalesmartlyChat.push("onUnRead", SalesmartlyCallback { payload ->
            val num = when (val raw = payload["num"]) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull() ?: 0
                else -> 0
            }
            _unreadCount.value = num
        })

        SalesmartlyChat.push("onReceiveMessage", SalesmartlyCallback {
            Log.d(TAG, "SaleSmartly agent message received")
        })

        SalesmartlyChat.push("onOpenChat", SalesmartlyCallback {
            _conversationOpen.value = true
        })

        SalesmartlyChat.push("onCloseChat", SalesmartlyCallback {
            _conversationOpen.value = false
        })

        SalesmartlyChat.push("onCollectionInfo", SalesmartlyCallback { payload ->
            val fields = payload["payload"] as? Map<*, *>
            val phone = fields?.get("phone") as? String
            if (!phone.isNullOrBlank()) {
                appsFlyerManager.logSupportPhoneSubmitted()
            }
        })

        runCatching {
            SalesmartlyChat.initialize(context.applicationContext, scriptUrl)
            initialized = true
            Log.d(TAG, "SaleSmartly initialize() called successfully")
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize SaleSmartlyChat", e)
        }
    }

    fun openConversation() {
        initializeIfNeeded()
        if (!_sdkReady.value) {
            Log.w(TAG, "openConversation ignored: SDK not ready")
            return
        }
        runCatching {
            SalesmartlyChat.openChat()
            _conversationOpen.value = true
        }.onFailure { error ->
            Log.e(TAG, "Failed to open SaleSmartly conversation", error)
        }
    }

    fun openSupportHome() {
        initializeIfNeeded()
        if (!_sdkReady.value) {
            Log.w(TAG, "openSupportHome ignored: SDK not ready")
            return
        }
        runCatching {
            // Respects the console's home-page setting and keeps Enter chat inside the SDK.
            SalesmartlyChat.open()
        }.onFailure { error ->
            Log.e(TAG, "Failed to open SaleSmartly home", error)
        }
    }

    fun goBackToHome() {
        runCatching {
            SalesmartlyChat.runtime().goBackToHome()
        }.onFailure { error ->
            Log.e(TAG, "Failed to return to SaleSmartly home", error)
        }
    }

    fun closeChat() {
        _conversationOpen.value = false
        runCatching {
            SalesmartlyChat.closeChat()
        }
    }

    /**
     * Identify the visitor for the support desk.
     * [userId] and [userName] are required by the SDK.
     */
    fun setLoginInfo(
        userId: String,
        userName: String,
        language: String = "en-US",
        phone: String = "",
        email: String = "",
        description: String = "Thrust Wealth App User",
        labelNames: List<String> = DEFAULT_LABELS,
        customFieldsExt: Map<String, Any> = emptyMap()
    ) {
        if (userId.isBlank() || userName.isBlank()) {
            Log.w(TAG, "setLoginInfo skipped: user_id and user_name are required")
            return
        }
        initializeIfNeeded()
        if (currentUserId == userId) {
            Log.d(TAG, "setLoginInfo skipped: identity is already active")
            return
        }
        currentUserId = userId
        runCatching {
            SalesmartlyChat.setLoginInfo(
                LoginInfo(
                    user_id = userId,
                    user_name = userName,
                    language = language,
                    phone = phone,
                    email = email,
                    description = description,
                    label_names = labelNames,
                    custom_fields_ext = customFieldsExt,
                )
            )
        }.onFailure {
            Log.w(TAG, "Failed to setLoginInfo", it)
        }
    }

    fun clearUser() {
        currentUserId = null
        runCatching {
            SalesmartlyChat.clearUser()
        }
    }

    fun runtime(): SalesmartlyRuntime {
        initializeIfNeeded()
        return SalesmartlyChat.runtime()
    }

    fun runtimeOrNull(): SalesmartlyRuntime? {
        return runCatching {
            initializeIfNeeded()
            SalesmartlyChat.runtime()
        }.getOrNull()
    }

    fun externalChannelUrl(channel: String): String? {
        val info = _uiState.value?.widgetInfo
            ?: runtimeOrNull()?.state?.value?.widgetInfo
            ?: return null
        val rawUrl = info.channelOpenUri(channel) ?: return null
        val uri = Uri.parse(rawUrl)
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("https", "http", "mailto", "tel", "tg", "whatsapp", "line")) return null
        return rawUrl
    }

    companion object {
        private const val TAG = "SalesmartlyManager"

        /** Labels that must already exist in the SaleSmartly console. */
        val DEFAULT_LABELS: List<String> = listOf("android", "thrust_wealth")

        fun newGuestUserId(): String = "tw_${UUID.randomUUID().toString().replace("-", "").take(24)}"
    }
}
