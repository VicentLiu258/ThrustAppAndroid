package com.thrustwealth.app.appsflyer

import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.thrustwealth.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppsFlyer attribution. Started only after the user accepts the privacy policy.
 */
@Singleton
class AppsFlyerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var initialized = false

    @Volatile
    private var started = false

    fun start(customerUserId: String) {
        val lib = AppsFlyerLib.getInstance()
        if (!initialized) {
            lib.setDebugLog(BuildConfig.DEBUG)
            lib.init(DEV_KEY, null, context)
            initialized = true
        }
        if (customerUserId.isNotBlank()) {
            lib.setCustomerUserId(customerUserId)
        }
        if (started) return
        lib.start(context)
        started = true
        Log.d(TAG, "AppsFlyer start requested")
    }

    /** Fired when a home-search result navigates the user into a tool. */
    fun logSearchJump(searchQuery: String, destination: String) {
        val values = hashMapOf<String, Any>(
            "af_search_string" to searchQuery,
            "af_content" to destination
        )
        AppsFlyerLib.getInstance().logEvent(context, EVENT_SEARCH_JUMP, values)
        Log.d(TAG, "logEvent $EVENT_SEARCH_JUMP query=$searchQuery destination=$destination")
    }

    /** SaleSmartly collection form was submitted with a non-empty phone field. */
    fun logSupportPhoneSubmitted() {
        if (!started) return
        AppsFlyerLib.getInstance().logEvent(context, EVENT_SUPPORT_PHONE_SUBMITTED, emptyMap())
        Log.d(TAG, "logEvent $EVENT_SUPPORT_PHONE_SUBMITTED")
    }

    companion object {
        private const val TAG = "AppsFlyer"
        const val DEV_KEY = "rk5rSYHn7GfJEcS3VXXJW3"
        const val EVENT_SEARCH_JUMP = "af_ic"
        const val EVENT_SUPPORT_PHONE_SUBMITTED = "af_whats_leads"
    }
}
