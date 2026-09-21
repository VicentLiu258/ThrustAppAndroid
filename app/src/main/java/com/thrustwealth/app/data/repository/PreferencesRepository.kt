package com.thrustwealth.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thrustwealth.app.data.model.AppPreferences
import com.thrustwealth.app.data.model.ThemeMode
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "thrust_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCOUNT_CURRENCY = stringPreferencesKey("account_currency")
        val DEFAULT_SYMBOL = stringPreferencesKey("default_symbol")
        val DEFAULT_LEVERAGE = stringPreferencesKey("default_leverage")
        val LOT_UNIT = stringPreferencesKey("lot_unit")
        val THEME = stringPreferencesKey("theme")
        val PRIVACY_CONSENT = booleanPreferencesKey("privacy_consent_accepted")
        val GUEST_USER_ID = stringPreferencesKey("guest_user_id")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            accountCurrency = prefs[Keys.ACCOUNT_CURRENCY] ?: "USD",
            defaultSymbol = prefs[Keys.DEFAULT_SYMBOL] ?: "XAUUSD",
            defaultLeverage = prefs[Keys.DEFAULT_LEVERAGE] ?: "1:100",
            lotUnitDisplay = prefs[Keys.LOT_UNIT] ?: "Standard",
            theme = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            privacyConsentAccepted = prefs[Keys.PRIVACY_CONSENT] ?: false,
            guestUserId = prefs[Keys.GUEST_USER_ID] ?: ""
        )
    }

    suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        context.dataStore.edit { prefs ->
            val current = AppPreferences(
                accountCurrency = prefs[Keys.ACCOUNT_CURRENCY] ?: "USD",
                defaultSymbol = prefs[Keys.DEFAULT_SYMBOL] ?: "XAUUSD",
                defaultLeverage = prefs[Keys.DEFAULT_LEVERAGE] ?: "1:100",
                lotUnitDisplay = prefs[Keys.LOT_UNIT] ?: "Standard",
                theme = runCatching {
                    ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.DARK.name)
                }.getOrDefault(ThemeMode.DARK),
                privacyConsentAccepted = prefs[Keys.PRIVACY_CONSENT] ?: false,
                guestUserId = prefs[Keys.GUEST_USER_ID] ?: ""
            )
            val updated = transform(current)
            prefs[Keys.ACCOUNT_CURRENCY] = updated.accountCurrency
            prefs[Keys.DEFAULT_SYMBOL] = updated.defaultSymbol
            prefs[Keys.DEFAULT_LEVERAGE] = updated.defaultLeverage
            prefs[Keys.LOT_UNIT] = updated.lotUnitDisplay
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.PRIVACY_CONSENT] = updated.privacyConsentAccepted
            prefs[Keys.GUEST_USER_ID] = updated.guestUserId
        }
    }

    suspend fun setCurrency(currency: String) = update { it.copy(accountCurrency = currency) }
    suspend fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }
    suspend fun setDefaultSymbol(symbol: String) = update { it.copy(defaultSymbol = symbol) }
    suspend fun setDefaultLeverage(leverage: String) = update { it.copy(defaultLeverage = leverage) }
    suspend fun setLotUnitDisplay(unit: String) = update { it.copy(lotUnitDisplay = unit) }

    suspend fun acceptPrivacyConsent() {
        update { current ->
            val guestId = current.guestUserId.ifBlank { SalesmartlyManager.newGuestUserId() }
            current.copy(privacyConsentAccepted = true, guestUserId = guestId)
        }
    }

    suspend fun ensureGuestUserId(): String {
        var resolved = ""
        update { current ->
            val id = current.guestUserId.ifBlank { SalesmartlyManager.newGuestUserId() }
            resolved = id
            current.copy(guestUserId = id)
        }
        return resolved
    }
}
