package com.thrustwealth.app.salesmartly

import android.content.Context
import android.util.Log
import com.salesmartly.chatwidget.api.LoginInfo
import com.salesmartly.chatwidget.api.SalesmartlyCallback
import com.salesmartly.chatwidget.api.SalesmartlyChat
import com.thrustwealth.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SaleSmartly Chat SDK facade.
 *
 * Must only call [initialize] after the user has accepted the privacy policy.
 */
@Singleton
class SalesmartlyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    @Volatile
    private var initialized = false

    @Volatile
    private var currentUserId: String? = null

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

        SalesmartlyChat.initialize(context.applicationContext, scriptUrl)
        initialized = true
        Log.d(TAG, "SaleSmartly initialize() called")
    }

    fun openChat() {
        if (!_sdkReady.value) {
            Log.w(TAG, "openChat ignored: SDK not ready")
            return
        }
        SalesmartlyChat.openChat()
    }

    fun closeChat() {
        SalesmartlyChat.closeChat()
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
        if (currentUserId == userId) {
            Log.d(TAG, "setLoginInfo skipped: identity is already active")
            return
        }
        currentUserId = userId
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
    }

    fun clearUser() {
        currentUserId = null
        SalesmartlyChat.clearUser()
    }

    fun runtime() = SalesmartlyChat.runtime()

    companion object {
        private const val TAG = "SalesmartlyManager"

        /** Labels that must already exist in the SaleSmartly console. */
        val DEFAULT_LABELS: List<String> = listOf("android", "thrust_wealth")

        fun newGuestUserId(): String = "tw_${UUID.randomUUID().toString().replace("-", "").take(24)}"
    }
}
