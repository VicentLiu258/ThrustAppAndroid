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
    var symbolExpanded by remember { mutableStateOf(false) }
    var leverageExpanded by remember { mutableStateOf(false) }
    var lotUnitExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(title = "Settings & Preferences")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsGroupTitle("Preferences")
            SettingsCard {
                DropdownRow(
                    label = "Account Currency",
                    value = when (prefs.accountCurrency) {
                        "USD" -> "USD ($)"
                        "EUR" -> "EUR (€)"
                        "GBP" -> "GBP (£)"
                        "JPY" -> "JPY (¥)"
                        else -> prefs.accountCurrency
                    },
                    expanded = currencyExpanded,
                    onToggle = { currencyExpanded = !currencyExpanded }
                )
                if (currencyExpanded) {
                    listOf("USD" to "USD ($)", "EUR" to "EUR (€)", "GBP" to "GBP (£)", "JPY" to "JPY (¥)").forEach { (code, label) ->
                        OptionItem(label, prefs.accountCurrency == code) {
                            viewModel.setCurrency(code)
                            currencyExpanded = false
                        }
                    }
                }
                DropdownRow(
                    label = "Default Theme",
                    value = when (prefs.theme) {
                        ThemeMode.DARK -> "Dark (Preferred)"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.AUTO -> "System Default"
                    },
                    expanded = themeExpanded,
                    onToggle = { themeExpanded = !themeExpanded }
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
                DropdownRow(
                    label = "Default Symbol",
                    value = prefs.defaultSymbol,
                    expanded = symbolExpanded,
                    onToggle = { symbolExpanded = !symbolExpanded }
                )
                if (symbolExpanded) {
                    instruments.ifEmpty { com.thrustwealth.app.engine.InstrumentConfig.DEFAULT_INSTRUMENTS }
                        .map { it.id }
                        .distinct()
                        .forEach { symbol ->
                            OptionItem(symbol, prefs.defaultSymbol == symbol) {
                                viewModel.setDefaultSymbol(symbol)
                                symbolExpanded = false
                            }
                        }
                }
                DropdownRow(
                    label = "Default Leverage",
                    value = prefs.defaultLeverage,
                    expanded = leverageExpanded,
                    onToggle = { leverageExpanded = !leverageExpanded }
                )
                if (leverageExpanded) {
                    listOf("1:50", "1:100", "1:200", "1:500").forEach { leverage ->
                        OptionItem(leverage, prefs.defaultLeverage == leverage) {
                            viewModel.setDefaultLeverage(leverage)
                            leverageExpanded = false
                        }
                    }
                }
                DropdownRow(
                    label = "Lot Unit Display",
                    value = prefs.lotUnitDisplay,
                    expanded = lotUnitExpanded,
                    onToggle = { lotUnitExpanded = !lotUnitExpanded }
                )
                if (lotUnitExpanded) {
                    listOf("Standard", "Mini", "Micro").forEach { unit ->
                        OptionItem(unit, prefs.lotUnitDisplay == unit) {
                            viewModel.setLotUnitDisplay(unit)
                            lotUnitExpanded = false
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle("Data Management")
            SettingsCard {
                NavRow("Export Calculations to CSV") { viewModel.exportCsv(context) }
                NavRow("Full App Backup (JSON)") { viewModel.exportJson(context) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showPurge(true) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Purge & Reset All App Data", color = colors.loss, fontSize = 15.sp)
                    Icon(Icons.Outlined.DeleteOutline, null, tint = colors.loss, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle("Help & Support")
            SettingsCard {
                NavRow("Customer Support") { onOpenSupport() }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle("Privacy & Compliance")
            SettingsCard {
                NavRow("Privacy Policy") { viewModel.openSheet("privacy") }
                NavRow("Terms of Service") { viewModel.openSheet("terms") }
                NavRow("About Thrust Wealth") { viewModel.openSheet("about") }
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
            Column(modifier = Modifier.padding(20.dp)) {
                when (sheet) {
                    "privacy" -> {
                        Text("Privacy Policy", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
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
                    }
                    "terms" -> {
                        Text("Terms of Service", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
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
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = ThrustTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgElevated, RoundedCornerShape(14.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun DropdownRow(label: String, value: String, expanded: Boolean, onToggle: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = colors.textSecondary, fontSize = 14.sp)
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun OptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) colors.brand.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.textPrimary, fontSize = 14.sp)
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = colors.brand, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 15.sp)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}
