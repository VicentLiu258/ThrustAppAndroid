package com.thrustwealth.app.ui.pnl

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thrustwealth.app.data.model.AppPreferences
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.PnlResult
import com.thrustwealth.app.data.model.TradeEntry
import com.thrustwealth.app.data.repository.InstrumentRepository
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.data.repository.TradeRepository
import com.thrustwealth.app.engine.CalculationEngine
import com.thrustwealth.app.engine.InstrumentConfig
import com.thrustwealth.app.ui.calculator.ActionChip
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.components.ComplianceDisclaimer
import com.thrustwealth.app.ui.components.DirectionSegmented
import com.thrustwealth.app.ui.components.FormGroup
import com.thrustwealth.app.ui.components.FormInput
import com.thrustwealth.app.ui.components.ResultShowcaseCard
import com.thrustwealth.app.ui.components.SymbolDropdownButton
import com.thrustwealth.app.ui.components.SymbolPickerSheet
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.util.ExportHelper
import com.thrustwealth.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PnlUiState(
    val symbol: String = "XAUUSD",
    val isLong: Boolean = true,
    val openPrice: String = "2350.00",
    val closePrice: String = "2360.00",
    val lots: String = "0.20",
    val instruments: List<Instrument> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val result: PnlResult = PnlResult(isValid = false),
    val showPicker: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class PnlCalculatorViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val preferencesRepository: PreferencesRepository,
    private val tradeRepository: TradeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PnlUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            instrumentRepository.seedIfNeeded()
            instrumentRepository.observeInstruments().collect { list ->
                _state.update { it.copy(instruments = list) }
                recalculate()
            }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _state.update { it.copy(preferences = prefs) }
                recalculate()
            }
        }
    }

    fun setSymbol(id: String) {
        val inst = _state.value.instruments.find { it.id == id }
        _state.update {
            it.copy(
                symbol = id,
                openPrice = inst?.defaultPrice?.toString() ?: it.openPrice,
                closePrice = inst?.let { i -> "%.${i.digits}f".format(i.defaultPrice * 1.004) } ?: it.closePrice,
                showPicker = false
            )
        }
        recalculate()
    }

    fun setLong(v: Boolean) { _state.update { it.copy(isLong = v) }; recalculate() }
    fun setOpen(v: String) { _state.update { it.copy(openPrice = v) }; recalculate() }
    fun setClose(v: String) { _state.update { it.copy(closePrice = v) }; recalculate() }
    fun setLots(v: String) { _state.update { it.copy(lots = v) }; recalculate() }
    fun showPicker(v: Boolean) { _state.update { it.copy(showPicker = v) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }

    fun reset() {
        val inst = currentInstrument()
        _state.update {
            it.copy(
                isLong = true,
                openPrice = inst?.defaultPrice?.toString() ?: "2350.00",
                closePrice = inst?.let { i -> "%.${i.digits}f".format(i.defaultPrice * 1.004) } ?: "2360.00",
                lots = "0.20",
                message = "P/L inputs reset"
            )
        }
        recalculate()
    }

    fun saveToJournal() {
        val s = _state.value
        val r = s.result
        if (!r.isValid) {
            _state.update { it.copy(message = "Invalid calculation") }
            return
        }
        viewModelScope.launch {
            tradeRepository.insert(
                TradeEntry(
                    symbol = s.symbol,
                    direction = if (s.isLong) "long" else "short",
                    lots = s.lots.toDoubleOrNull() ?: 0.0,
                    entryPrice = s.openPrice.toDoubleOrNull() ?: 0.0,
                    exitPrice = s.closePrice.toDoubleOrNull() ?: 0.0,
                    pnl = r.profitAmount,
                    note = "From P/L Calculator"
                )
            )
            _state.update { it.copy(message = "Saved to Trade Journal") }
        }
    }

    fun currentInstrument(): Instrument? =
        _state.value.instruments.find { it.id == _state.value.symbol }
            ?: InstrumentConfig.DEFAULT_INSTRUMENTS.find { it.id == _state.value.symbol }

    private fun recalculate() {
        val s = _state.value
        val result = CalculationEngine.calculateProfitLoss(
            instrument = currentInstrument(),
            direction = if (s.isLong) "long" else "short",
            openPrice = s.openPrice.toDoubleOrNull() ?: 0.0,
            closePrice = s.closePrice.toDoubleOrNull() ?: 0.0,
            lotSize = s.lots.toDoubleOrNull() ?: 0.0,
            accountCurrency = s.preferences.accountCurrency
        )
        _state.update { it.copy(result = result) }
    }
}

@Composable
fun PnlCalculatorScreen(
    onBack: () -> Unit,
    viewModel: PnlCalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val currency = state.preferences.accountCurrency
    val result = state.result

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(
            title = "P/L Calculator",
            onBack = onBack,
            actions = {
                IconButton(onClick = { viewModel.reset() }) {
                    Icon(Icons.Outlined.DeleteOutline, "Reset", tint = colors.textPrimary)
                }
                IconButton(onClick = {
                    ExportHelper.exportPnlCsv(
                        context, state.symbol, if (state.isLong) "long" else "short",
                        state.openPrice.toDoubleOrNull() ?: 0.0,
                        state.closePrice.toDoubleOrNull() ?: 0.0,
                        state.lots.toDoubleOrNull() ?: 0.0,
                        result.profitAmount, result.profitPips, result.returnPercent
                    )
                }) {
                    Icon(Icons.Outlined.FileDownload, "Export", tint = colors.textPrimary)
                }
                IconButton(onClick = { viewModel.saveToJournal() }) {
                    Icon(Icons.Outlined.Add, "Save", tint = colors.textPrimary)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val inst = viewModel.currentInstrument()
            FormGroup(
                label = "Instrument",
                hint = inst?.let { "Contract: ${it.contractSize.toInt()} ${it.unit} / lot" }
            ) {
                SymbolDropdownButton(
                    symbol = inst?.let { "${it.id} — ${it.name}" } ?: state.symbol,
                    onClick = { viewModel.showPicker(true) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FormGroup(label = "Direction") {
                DirectionSegmented(isLong = state.isLong, onChange = viewModel::setLong)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroup(label = "Entry Price", rootModifier = Modifier.weight(1f)) {
                    FormInput(value = state.openPrice, onValueChange = viewModel::setOpen)
                }
                FormGroup(label = "Exit Price", rootModifier = Modifier.weight(1f)) {
                    FormInput(value = state.closePrice, onValueChange = viewModel::setClose)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FormGroup(label = "Position Size (Lots)") {
                FormInput(value = state.lots, onValueChange = viewModel::setLots, suffix = "lots")
            }
            Spacer(modifier = Modifier.height(20.dp))

            val pnlColor = when {
                !result.isValid -> colors.textMuted
                result.isProfit -> colors.profit
                else -> colors.loss
            }
            ResultShowcaseCard(
                badge = "Calculated P/L",
                heroTitle = "Profit / Loss",
                heroValue = if (result.isValid) FormatUtils.formatSignedMoney(result.profitAmount, currency) else "—",
                heroValueColor = pnlColor,
                stats = listOf(
                    "Price Distance" to if (result.isValid) {
                        "${if (result.profitPips >= 0) "+" else ""}${result.profitPips} pips (${if (result.priceDelta >= 0) "+" else ""}${result.priceDelta})"
                    } else "—",
                    "Return on Notional" to if (result.isValid) {
                        "${if (result.returnPercent >= 0) "+" else ""}${FormatUtils.formatPercent(result.returnPercent, 2)}"
                    } else "—"
                ),
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip(
                            label = "Reset",
                            icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = colors.loss) },
                            onClick = { viewModel.reset() },
                            modifier = Modifier.weight(1f),
                            borderColor = colors.loss.copy(alpha = 0.4f)
                        )
                        ActionChip(
                            label = "Export",
                            icon = { Icon(Icons.Outlined.FileDownload, null, tint = colors.textSecondary) },
                            onClick = {
                                ExportHelper.exportPnlCsv(
                                    context, state.symbol, if (state.isLong) "long" else "short",
                                    state.openPrice.toDoubleOrNull() ?: 0.0,
                                    state.closePrice.toDoubleOrNull() ?: 0.0,
                                    state.lots.toDoubleOrNull() ?: 0.0,
                                    result.profitAmount, result.profitPips, result.returnPercent
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ActionChip(
                            label = "Save",
                            icon = { Icon(Icons.Outlined.Add, null, tint = androidx.compose.ui.graphics.Color.White) },
                            onClick = { viewModel.saveToJournal() },
                            modifier = Modifier.weight(1f),
                            filled = true
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ComplianceDisclaimer(
                text = "For calculation purposes only. Not investment advice. Calculation assumes zero slippage and benchmark spreads."
            )
        }
    }

    if (state.showPicker) {
        SymbolPickerSheet(
            instruments = state.instruments.ifEmpty { InstrumentConfig.DEFAULT_INSTRUMENTS },
            selectedId = state.symbol,
            onSelect = { viewModel.setSymbol(it.id) },
            onDismiss = { viewModel.showPicker(false) }
        )
    }
}
