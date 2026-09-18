package com.thrustwealth.app.ui.calculator

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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thrustwealth.app.data.model.CalcMode
import com.thrustwealth.app.engine.InstrumentConfig
import com.thrustwealth.app.ui.components.ComplianceDisclaimer
import com.thrustwealth.app.ui.components.DirectionSegmented
import com.thrustwealth.app.ui.components.FormGroup
import com.thrustwealth.app.ui.components.FormInput
import com.thrustwealth.app.ui.components.QuickSymbolBar
import com.thrustwealth.app.ui.components.ResultShowcaseCard
import com.thrustwealth.app.ui.components.SearchField
import com.thrustwealth.app.ui.components.SegmentedControl
import com.thrustwealth.app.ui.components.SymbolDropdownButton
import com.thrustwealth.app.ui.components.SymbolPickerSheet
import com.thrustwealth.app.ui.theme.BrandPrimary
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.util.ExportHelper
import com.thrustwealth.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onOpenHistory: () -> Unit,
    onOpenPnl: () -> Unit,
    onOpenSpecs: () -> Unit,
    onOpenJournal: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val instrument = viewModel.currentInstrument()
    val currency = state.preferences.accountCurrency

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thrust Wealth",
                color = BrandPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Outlined.History, contentDescription = "History", tint = colors.textPrimary)
            }
            IconButton(onClick = { viewModel.saveRecord() }) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = colors.textPrimary)
            }
            IconButton(onClick = { viewModel.showMoreSheet(true) }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = colors.textPrimary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SearchField(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                placeholder = "Search tools (P/L, Specs, Journal)..."
            )

            if (state.searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SearchResults(
                    query = state.searchQuery,
                    onPnl = onOpenPnl,
                    onSpecs = onOpenSpecs,
                    onJournal = onOpenJournal
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                SegmentedControl(
                    options = listOf("By Risk", "By Lots"),
                    selectedIndex = if (state.mode == CalcMode.BY_RISK) 0 else 1,
                    onSelect = { viewModel.setMode(if (it == 0) CalcMode.BY_RISK else CalcMode.BY_LOTS) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                FormGroup(
                    label = "Instrument / Symbol",
                    hint = instrument?.let { "Contract: ${it.contractSize.toInt()} ${it.unit} / lot" }
                ) {
                    SymbolDropdownButton(
                        symbol = state.symbol,
                        onClick = { viewModel.showSymbolPicker(true) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                FormGroup(label = "Account Balance", hint = "Base Currency: $currency") {
                    FormInput(
                        value = state.balance,
                        onValueChange = viewModel::setBalance,
                        prefix = FormatUtils.currencySymbol(currency)
                    )
                }

                if (state.mode == CalcMode.BY_RISK) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormGroup(label = "Risk %", rootModifier = Modifier.weight(1f)) {
                            FormInput(
                                value = state.riskPercent,
                                onValueChange = viewModel::setRiskPercent,
                                suffix = "%"
                            )
                        }
                        FormGroup(label = "Risk Amount", rootModifier = Modifier.weight(1f)) {
                            FormInput(
                                value = state.riskAmount,
                                onValueChange = viewModel::setRiskAmount,
                                prefix = FormatUtils.currencySymbol(currency)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5, 1.0, 2.0, 3.0).forEach { preset ->
                            val active = state.riskPercent.toDoubleOrNull() == preset
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (active) colors.brand else colors.bgInput,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) colors.brand else colors.borderMedium,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.setPresetRisk(preset) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${preset}%",
                                    color = if (active) Color.White else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    FormGroup(label = "Lot Size", hint = "Standard Lots") {
                        FormInput(
                            value = state.lotSize,
                            onValueChange = viewModel::setLotSize,
                            suffix = "lots"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                FormGroup(
                    label = "Stop Loss (Pips)",
                    hint = instrument?.let { "1 pip = ${it.pipSize}" }
                ) {
                    FormInput(
                        value = state.stopPips,
                        onValueChange = viewModel::setStopPips,
                        suffix = "pips"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormGroup(label = "Entry Price", hint = "Optional", rootModifier = Modifier.weight(1f)) {
                        FormInput(
                            value = state.entryPrice,
                            onValueChange = viewModel::setEntryPrice
                        )
                    }
                    FormGroup(label = "Direction", rootModifier = Modifier.weight(1f)) {
                        DirectionSegmented(
                            isLong = state.isLong,
                            onChange = viewModel::setDirection
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                val result = state.result
                val heroValue = if (result.isValid) {
                    "${FormatUtils.formatLots(result.suggestedLots)} Lots"
                } else "—"
                val heroSub = if (result.isValid) {
                    "(= ${result.miniLots} Mini / ${result.microLots} Micro lots)"
                } else result.error
                val digits = instrument?.digits ?: 2
                ResultShowcaseCard(
                    badge = "Sizing Result",
                    heroTitle = if (state.mode == CalcMode.BY_RISK) "Suggested Lot Size" else "Lot Size (Fixed)",
                    heroValue = heroValue,
                    heroSub = heroSub,
                    heroValueColor = if (result.isValid) colors.textPrimary else colors.textMuted,
                    stats = listOf(
                        "Risk Amount" to FormatUtils.formatMoney(result.riskAmount, currency),
                        "Risk %" to FormatUtils.formatPercent(result.riskPercent),
                        "Pip Value" to "${FormatUtils.formatMoney(result.pipValue, currency)} / pip",
                        "Notional Value" to FormatUtils.formatMoney(result.notionalValue, currency, 0),
                        "Stop Loss Price" to (
                            result.stopLossPrice?.let {
                                "${FormatUtils.formatPrice(it, digits)} (${if (state.isLong) "Long" else "Short"})"
                            } ?: "—"
                            )
                    ),
                    actions = {
                        ResultActions(
                            onReset = { viewModel.reset() },
                            onExport = {
                                ExportHelper.exportSingleCalculationCsv(
                                    context = context,
                                    symbol = state.symbol,
                                    mode = state.mode.name,
                                    balance = state.balance.toDoubleOrNull() ?: 0.0,
                                    riskPercent = result.riskPercent,
                                    riskAmount = result.riskAmount,
                                    stopPips = state.stopPips.toDoubleOrNull() ?: 0.0,
                                    lotSize = result.suggestedLots,
                                    pipValue = result.pipValue,
                                    notional = result.notionalValue
                                )
                            },
                            onSave = { viewModel.saveRecord() }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                ComplianceDisclaimer()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (state.searchQuery.isBlank()) {
            QuickSymbolBar(
                symbols = InstrumentConfig.QUICK_SYMBOLS,
                selected = state.symbol,
                onSelect = viewModel::setSymbol
            )
        }
    }

    if (state.showSymbolPicker) {
        SymbolPickerSheet(
            instruments = state.instruments.ifEmpty { InstrumentConfig.DEFAULT_INSTRUMENTS },
            selectedId = state.symbol,
            onSelect = { viewModel.setSymbol(it.id) },
            onDismiss = { viewModel.showSymbolPicker(false) }
        )
    }

    if (state.showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showMoreSheet(false) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.bgSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Calculator Options", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                SheetAction("Reset Calculator") {
                    viewModel.reset()
                    viewModel.showMoreSheet(false)
                }
                SheetAction("Export Calculation (CSV)") {
                    val r = state.result
                    ExportHelper.exportSingleCalculationCsv(
                        context, state.symbol, state.mode.name,
                        state.balance.toDoubleOrNull() ?: 0.0, r.riskPercent, r.riskAmount,
                        state.stopPips.toDoubleOrNull() ?: 0.0, r.suggestedLots, r.pipValue, r.notionalValue
                    )
                    viewModel.showMoreSheet(false)
                }
                SheetAction("Share Calculation Summary") {
                    val r = state.result
                    ExportHelper.shareText(
                        context,
                        "Thrust Wealth Calculation",
                        "${state.symbol}: ${FormatUtils.formatLots(r.suggestedLots)} lots | Risk ${FormatUtils.formatMoney(r.riskAmount, currency)} (${FormatUtils.formatPercent(r.riskPercent)}) | SL ${state.stopPips} pips"
                    )
                    viewModel.showMoreSheet(false)
                }
                SheetAction("Calculation Reference") {
                    ExportHelper.shareText(
                        context,
                        "Position Sizing Formula",
                        "Suggested Lots = Risk Amount ÷ (Stop Loss Pips × Pip Value)\n\nGold (XAUUSD): 100 oz/lot, 1 pip = 0.10 ($10/pip per standard lot)."
                    )
                    viewModel.showMoreSheet(false)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SearchResults(
    query: String,
    onPnl: () -> Unit,
    onSpecs: () -> Unit,
    onJournal: () -> Unit
) {
    val colors = ThrustTheme.colors
    val tools = listOf(
        Triple("P/L Calculator", "Profit / loss by open & close", onPnl),
        Triple("Market Specs", "Contract size & pip value", onSpecs),
        Triple("Trade Journal", "Local trade log & stats", onJournal)
    ).filter {
        it.first.contains(query, true) || it.second.contains(query, true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tools.isEmpty()) {
            Text("No tools found", color = colors.textTertiary, fontSize = 13.sp)
        } else {
            tools.forEach { (title, desc, action) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgElevated, RoundedCornerShape(12.dp))
                        .clickable(onClick = action)
                        .padding(14.dp)
                ) {
                    Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = colors.textTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ResultActions(
    onReset: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChip(
            label = "Reset",
            icon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp), tint = colors.loss) },
            onClick = onReset,
            modifier = Modifier.weight(1f),
            borderColor = colors.loss.copy(alpha = 0.4f)
        )
        ActionChip(
            label = "Export",
            icon = { Icon(Icons.Outlined.FileDownload, null, Modifier.size(16.dp), tint = colors.textSecondary) },
            onClick = onExport,
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label = "Save",
            icon = { Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(16.dp), tint = Color.White) },
            onClick = onSave,
            modifier = Modifier.weight(1f),
            filled = true
        )
    }
}

@Composable
fun ActionChip(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    borderColor: Color? = null
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = modifier
            .height(40.dp)
            .background(
                if (filled) colors.brand else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .border(1.dp, borderColor ?: colors.borderMedium, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            color = if (filled) Color.White else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    val colors = ThrustTheme.colors
    Text(
        text = label,
        color = colors.textPrimary,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
