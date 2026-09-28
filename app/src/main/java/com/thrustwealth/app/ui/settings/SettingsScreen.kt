package com.thrustwealth.app.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thrustwealth.app.data.model.AppPreferences
import com.thrustwealth.app.data.model.ThemeMode
import com.thrustwealth.app.data.repository.HistoryRepository
import com.thrustwealth.app.data.repository.InstrumentRepository
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.data.repository.TradeRepository
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.components.ComplianceDisclaimer
import com.thrustwealth.app.ui.theme.BrandPrimary
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.util.ExportHelper
import com.thrustwealth.app.util.LegalConstants
import com.thrustwealth.app.util.openWebUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val historyRepository: HistoryRepository,
    private val tradeRepository: TradeRepository,
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {
    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    private val _sheet = MutableStateFlow<String?>(null)
    val sheet = _sheet.asStateFlow()
    private val _showPurge = MutableStateFlow(false)
    val showPurge = _showPurge.asStateFlow()

    fun openSheet(id: String?) = _sheet.update { id }
    fun showPurge(v: Boolean) = _showPurge.update { v }

    fun setCurrency(currency: String) {
        viewModelScope.launch { preferencesRepository.setCurrency(currency) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setTheme(theme) }
    }

    fun setDefaultSymbol(symbol: String) {
        viewModelScope.launch { preferencesRepository.setDefaultSymbol(symbol) }
    }

    fun setDefaultLeverage(leverage: String) {
        viewModelScope.launch { preferencesRepository.setDefaultLeverage(leverage) }
    }

    fun setLotUnitDisplay(unit: String) {
        viewModelScope.launch { preferencesRepository.setLotUnitDisplay(unit) }
    }

    fun acceptPrivacyConsent() {
        viewModelScope.launch { preferencesRepository.acceptPrivacyConsent() }
    }

    suspend fun ensureGuestUserId(): String = preferencesRepository.ensureGuestUserId()

    fun exportCsv(context: android.content.Context) {
        viewModelScope.launch {
            ExportHelper.exportHistoryCsv(context, historyRepository.getAll())
        }
    }

    fun exportJson(context: android.content.Context) {
        viewModelScope.launch {
            ExportHelper.exportFullBackupJson(
                context,
                historyRepository.getAll(),
                tradeRepository.getAll(),
                instrumentRepository.getAll()
            )
        }
    }

    fun purgeAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
            tradeRepository.clearAll()
            _showPurge.update { false }
        }
    }

    val instruments = instrumentRepository.observeInstruments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenSupport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val sheet by viewModel.sheet.collectAsState()
    val showPurge by viewModel.showPurge.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val instruments by viewModel.instruments.collectAsState()
    var currencyExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(title = "Settings & Preferences")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Preferences
            SettingsGroupTitle("Preferences")
            SettingsCard {
                SettingsRowItem(
                    label = "Account Currency",
                    sub = "Base currency for calculations",
                    trailing = {
                        CustomDropdownTrigger(
                            value = when (prefs.accountCurrency) {
                                "USD" -> "USD ($)"
                                "EUR" -> "EUR (€)"
                                "GBP" -> "GBP (£)"
                                "JPY" -> "JPY (¥)"
                                else -> prefs.accountCurrency
                            },
                            onClick = { currencyExpanded = !currencyExpanded }
                        )
                    }
                )
                if (currencyExpanded) {
                    listOf("USD" to "USD ($)", "EUR" to "EUR (€)", "GBP" to "GBP (£)", "JPY" to "JPY (¥)").forEach { (code, label) ->
                        OptionItem(label, prefs.accountCurrency == code) {
                            viewModel.setCurrency(code)
                            currencyExpanded = false
                        }
                    }
                }

                SettingsDivider()

                SettingsRowItem(
                    label = "Default Theme",
                    sub = "Color scheme preference",
                    trailing = {
                        CustomDropdownTrigger(
                            value = when (prefs.theme) {
                                ThemeMode.DARK -> "Dark (Preferred)"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.AUTO -> "System Default"
                            },
                            onClick = { themeExpanded = !themeExpanded }
                        )
                    }
                )
                if (themeExpanded) {
                    listOf(
                        ThemeMode.DARK to "Dark (Preferred)",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.AUTO to "System Default"
                    ).forEach { (mode, label) ->
                        OptionItem(label, prefs.theme == mode) {
                            viewModel.setTheme(mode)
                            themeExpanded = false
                        }
                    }
                }

                SettingsDivider()

                // 二级入口: 交易默认配置 (保留原有品种、杠杆、单位设置，避免挤占主界面)
                SettingsRowItem(
                    label = "Trading Parameters",
                    sub = "Default symbol, leverage & display units",
                    onClick = { viewModel.openSheet("trading_defaults") },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Data Management
            SettingsGroupTitle("Data Management")
            SettingsCard {
                SettingsRowItem(
                    label = "Export Calculations to CSV",
                    sub = "Download complete calculation log",
                    onClick = { viewModel.exportCsv(context) },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SettingsDivider()

                SettingsRowItem(
                    label = "Full App Backup (JSON)",
                    sub = "Export journal trades, specs, and preferences",
                    onClick = { viewModel.exportJson(context) },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SettingsDivider()

                SettingsRowItem(
                    label = "Purge & Reset All App Data",
                    sub = "Clear all local calculations and trades",
                    isDanger = true,
                    onClick = { viewModel.showPurge(true) },
                    trailing = {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = colors.loss,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Privacy & Compliance
            SettingsGroupTitle("Privacy & Compliance")
            SettingsCard {
                SettingsRowItem(
                    label = "Privacy Policy",
                    sub = "Zero telemetry & local-only storage guarantee",
                    onClick = { viewModel.openSheet("privacy") },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SettingsDivider()

                SettingsRowItem(
                    label = "Terms of Service",
                    sub = "Calculator disclaimer & computational terms",
                    onClick = { viewModel.openSheet("terms") },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SettingsDivider()

                SettingsRowItem(
                    label = "About Thrust Wealth",
                    sub = "Version 1.0.0 — Pure Math Engine",
                    onClick = { viewModel.openSheet("about") },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SettingsDivider()

                // 保留客服支持二级入口
                SettingsRowItem(
                    label = "Customer Support",
                    sub = "Contact help desk & product support",
                    onClick = { onOpenSupport() },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            ComplianceDisclaimer(
                text = "For computational and educational purposes only. This tool does not constitute investment advice, financial promotion, or a guarantee of returns. Trading leveraged instruments involves significant risk of capital loss."
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.openSheet(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.bgSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (sheet) {
                    "trading_defaults" -> {
                        Text("Trading Parameters", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsGroupTitle("Default Symbol")
                        SettingsCard {
                            instruments.ifEmpty { com.thrustwealth.app.engine.InstrumentConfig.DEFAULT_INSTRUMENTS }
                                .map { it.id }
                                .distinct()
                                .forEach { symbol ->
                                    OptionItem(symbol, prefs.defaultSymbol == symbol) {
                                        viewModel.setDefaultSymbol(symbol)
                                    }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsGroupTitle("Default Leverage")
                        SettingsCard {
                            listOf("1:50", "1:100", "1:200", "1:500").forEach { leverage ->
                                OptionItem(leverage, prefs.defaultLeverage == leverage) {
                                    viewModel.setDefaultLeverage(leverage)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsGroupTitle("Lot Unit Display")
                        SettingsCard {
                            listOf("Standard", "Mini", "Micro").forEach { unit ->
                                OptionItem(unit, prefs.lotUnitDisplay == unit) {
                                    viewModel.setLotUnitDisplay(unit)
                                }
                            }
                        }
                    }
                    "privacy" -> {
                        Text("Privacy Policy", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.brand.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { context.openWebUrl(LegalConstants.PRIVACY_POLICY_URL) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Online Privacy Policy", color = colors.brand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(LegalConstants.PRIVACY_POLICY_URL, color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open", tint = colors.brand, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Your Privacy", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Thrust Wealth stores calculation history and journal records locally on your device. Trade entries, balance numbers, and calculation parameters are not uploaded for cloud sync.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Data Retention", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "You may export or permanently purge all history and journal records at any time from the Settings tab.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Third-Party SDK — SaleSmartly", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "SDK name: SaleSmartly Android SDK\n" +
                                "Company: Guangzhou Biaopin Software Co., Ltd.\n" +
                                "Purpose: In-app customer support chat\n" +
                                "Data collected: Device/system info (OS type & version, app package & version, device type/manufacturer/model, network type), IP address, and images/videos you choose to upload in chat\n" +
                                "Permissions: Internet (required); media access & notifications (optional)\n" +
                                "Collection method: SDK self-collection after privacy consent\n" +
                                "Website: https://www.salesmartly.cn\n" +
                                "SDK privacy policy: https://www.salesmartly.com/html/sdk/privacy_native_cn.html",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Third-Party SDK — AppsFlyer", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "SDK name: AppsFlyer Android SDK\n" +
                                "Company: AppsFlyer Inc.\n" +
                                "Purpose: Search attribution and marketing analytics\n" +
                                "Data collected: Device identifiers (GAID, Android ID if permitted), app install/launch timestamps, IP address, OS version, device model\n" +
                                "Permissions: Internet, Access Network State\n" +
                                "Collection method: SDK self-collection after user consent\n" +
                                "Website: https://www.appsflyer.com\n" +
                                "SDK privacy policy: https://www.appsflyer.com/legal/services-privacy-policy/",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    "terms" -> {
                        Text("Terms of Service", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.brand.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { context.openWebUrl(LegalConstants.TERMS_OF_USE_URL) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Online Terms of Service", color = colors.brand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(LegalConstants.TERMS_OF_USE_URL, color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open", tint = colors.brand, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("About This Tool", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Thrust Wealth provides mathematical calculations based on standard contract specifications. It does not provide trade executions, investment recommendations, or portfolio guarantees.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Risk of Capital Loss", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Trading financial contracts (Forex, CFDs, precious metals) involves significant market risk. Users are solely responsible for setting appropriate stop-loss orders with their executing brokers.",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    "about" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Thrust Wealth", color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Risk & Position Utility", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Version 1.0.0 (Build 2026.09)", color = colors.textTertiary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "A professional risk management and position sizing utility designed for active traders who prioritise disciplined risk management.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPurge) {
        AlertDialog(
            onDismissRequest = { viewModel.showPurge(false) },
            title = { Text("Purge All Data?") },
            text = { Text("This will permanently delete all calculation history and trade journal records.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.purgeAll()
                    Toast.makeText(context, "All data purged", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Purge", color = colors.loss)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showPurge(false) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = ThrustTheme.colors.textTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = ThrustTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, shape)
            .border(1.dp, colors.borderSubtle, shape)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ThrustTheme.colors.borderSubtle)
    )
}

@Composable
private fun SettingsRowItem(
    label: String,
    sub: String? = null,
    isDanger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                color = if (isDanger) colors.loss else colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (sub != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = colors.textTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
        trailing()
    }
}

@Composable
private fun CustomDropdownTrigger(
    value: String,
    onClick: () -> Unit
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(colors.bgElevated, RoundedCornerShape(10.dp))
            .border(1.dp, colors.borderMedium, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun OptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) colors.brand.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 14.sp)
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = colors.brand, modifier = Modifier.size(18.dp))
        }
    }
}
