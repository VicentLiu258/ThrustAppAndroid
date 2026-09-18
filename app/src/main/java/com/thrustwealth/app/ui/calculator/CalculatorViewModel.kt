package com.thrustwealth.app.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thrustwealth.app.data.model.AppPreferences
import com.thrustwealth.app.data.model.CalcMode
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.LotSizeResult
import com.thrustwealth.app.data.repository.HistoryRepository
import com.thrustwealth.app.data.repository.InstrumentRepository
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.engine.CalculationEngine
import com.thrustwealth.app.engine.InstrumentConfig
import com.thrustwealth.app.salesmartly.SalesmartlyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalculatorUiState(
    val mode: CalcMode = CalcMode.BY_RISK,
    val symbol: String = "XAUUSD",
    val balance: String = "10000",
    val riskPercent: String = "1.0",
    val riskAmount: String = "100.00",
    val lotSize: String = "0.20",
    val stopPips: String = "50",
    val entryPrice: String = "2350.00",
    val isLong: Boolean = true,
    val searchQuery: String = "",
    val showSymbolPicker: Boolean = false,
    val showMoreSheet: Boolean = false,
    val instruments: List<Instrument> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val result: LotSizeResult = LotSizeResult(isValid = false),
    val message: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val historyRepository: HistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val salesmartlyManager: SalesmartlyManager
) : ViewModel() {

    val unreadChatCount: StateFlow<Int> = salesmartlyManager.unreadCount

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    private val inputs = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            instrumentRepository.seedIfNeeded()
        }
        viewModelScope.launch {
            combine(
                instrumentRepository.observeInstruments(),
                preferencesRepository.preferences
            ) { instruments, prefs ->
                instruments to prefs
            }.collect { (instruments, prefs) ->
                _state.update { current ->
                    val symbol = if (instruments.any { it.id == current.symbol }) {
                        current.symbol
                    } else {
                        prefs.defaultSymbol
                    }
                    val instrument = instruments.find { it.id == symbol }
                    current.copy(
                        instruments = instruments,
                        preferences = prefs,
                        symbol = symbol,
                        entryPrice = if (current.entryPrice == "2350.00" && instrument != null && symbol != "XAUUSD") {
                            instrument.defaultPrice.toString()
                        } else current.entryPrice
                    )
                }
                recalculate()
            }
        }
        viewModelScope.launch {
            inputs.debounce(300).collect { recalculate() }
        }
    }

    fun setMode(mode: CalcMode) {
        _state.update { it.copy(mode = mode) }
        bump()
    }

    fun setSymbol(symbol: String) {
        val instrument = _state.value.instruments.find { it.id == symbol }
        _state.update {
            it.copy(
                symbol = symbol,
                entryPrice = instrument?.defaultPrice?.toString() ?: it.entryPrice,
                showSymbolPicker = false
            )
        }
        bump()
    }

    fun setBalance(value: String) {
        _state.update { state ->
            val balance = value.toDoubleOrNull() ?: 0.0
            val riskPct = state.riskPercent.toDoubleOrNull() ?: 0.0
            val riskAmt = if (state.mode == CalcMode.BY_RISK) {
                "%.2f".format(balance * riskPct / 100.0)
            } else state.riskAmount
            state.copy(balance = value, riskAmount = riskAmt)
        }
        bump()
    }

    fun setRiskPercent(value: String) {
        _state.update { state ->
            val balance = state.balance.toDoubleOrNull() ?: 0.0
            val riskPct = value.toDoubleOrNull() ?: 0.0
            state.copy(
                riskPercent = value,
                riskAmount = "%.2f".format(balance * riskPct / 100.0)
            )
        }
        bump()
    }

    fun setRiskAmount(value: String) {
        _state.update { state ->
            val balance = state.balance.toDoubleOrNull() ?: 0.0
            val riskAmt = value.toDoubleOrNull() ?: 0.0
            val riskPct = if (balance > 0) riskAmt / balance * 100.0 else 0.0
            state.copy(
                riskAmount = value,
                riskPercent = "%.2f".format(riskPct)
            )
        }
        bump()
    }

    fun setPresetRisk(percent: Double) {
        setRiskPercent("%.1f".format(percent))
    }

    fun setLotSize(value: String) {
        _state.update { it.copy(lotSize = value) }
        bump()
    }

    fun setStopPips(value: String) {
        _state.update { it.copy(stopPips = value) }
        bump()
    }

    fun setEntryPrice(value: String) {
        _state.update { it.copy(entryPrice = value) }
        bump()
    }

    fun setDirection(isLong: Boolean) {
        _state.update { it.copy(isLong = isLong) }
        bump()
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun showSymbolPicker(show: Boolean) {
        _state.update { it.copy(showSymbolPicker = show) }
    }

    fun showMoreSheet(show: Boolean) {
        _state.update { it.copy(showMoreSheet = show) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun reset() {
        val instrument = currentInstrument()
        _state.update {
            it.copy(
                mode = CalcMode.BY_RISK,
                balance = "10000",
                riskPercent = "1.0",
                riskAmount = "100.00",
                lotSize = "0.20",
                stopPips = "50",
                entryPrice = instrument?.defaultPrice?.toString() ?: "2350.00",
                isLong = true,
                message = "Calculator reset"
            )
        }
        bump()
    }

    fun saveRecord() {
        val s = _state.value
        val result = s.result
        if (!result.isValid) {
            _state.update { it.copy(message = "Nothing to save — check inputs") }
            return
        }
        viewModelScope.launch {
            historyRepository.save(
                CalculationRecord(
                    symbol = s.symbol,
                    mode = s.mode.name,
                    balance = s.balance.toDoubleOrNull() ?: 0.0,
                    riskPercent = result.riskPercent,
                    riskAmount = result.riskAmount,
                    stopPips = s.stopPips.toDoubleOrNull() ?: 0.0,
                    entryPrice = s.entryPrice.toDoubleOrNull() ?: 0.0,
                    direction = if (s.isLong) "long" else "short",
                    lotSize = result.suggestedLots,
                    pipValue = result.pipValue,
                    notionalValue = result.notionalValue,
                    stopLossPrice = result.stopLossPrice
                )
            )
            _state.update { it.copy(message = "Saved to history") }
        }
    }

    fun restoreFromHistory(record: CalculationRecord) {
        _state.update {
            it.copy(
                symbol = record.symbol,
                mode = runCatching { CalcMode.valueOf(record.mode) }.getOrDefault(CalcMode.BY_RISK),
                balance = record.balance.toString(),
                riskPercent = record.riskPercent.toString(),
                riskAmount = "%.2f".format(record.riskAmount),
                lotSize = "%.2f".format(record.lotSize),
                stopPips = record.stopPips.toString(),
                entryPrice = record.entryPrice.toString(),
                isLong = record.direction.equals("long", ignoreCase = true),
                message = "Restored from history"
            )
        }
        bump()
    }

    fun currentInstrument(): Instrument? =
        _state.value.instruments.find { it.id == _state.value.symbol }
            ?: InstrumentConfig.DEFAULT_INSTRUMENTS.find { it.id == _state.value.symbol }

    private fun bump() {
        inputs.update { it + 1 }
    }

    private fun recalculate() {
        val s = _state.value
        val instrument = currentInstrument()
        val currency = s.preferences.accountCurrency
        val direction = if (s.isLong) "long" else "short"
        val result = when (s.mode) {
            CalcMode.BY_RISK -> CalculationEngine.calculateLotSize(
                balance = s.balance.toDoubleOrNull() ?: 0.0,
                riskPercent = s.riskPercent.toDoubleOrNull() ?: 0.0,
                stopPips = s.stopPips.toDoubleOrNull() ?: 0.0,
                instrument = instrument,
                entryPrice = s.entryPrice.toDoubleOrNull() ?: 0.0,
                direction = direction,
                accountCurrency = currency
            )
            CalcMode.BY_LOTS -> CalculationEngine.calculateRiskFromLots(
                balance = s.balance.toDoubleOrNull() ?: 0.0,
                lotSize = s.lotSize.toDoubleOrNull() ?: 0.0,
                stopPips = s.stopPips.toDoubleOrNull() ?: 0.0,
                instrument = instrument,
                entryPrice = s.entryPrice.toDoubleOrNull() ?: 0.0,
                direction = direction,
                accountCurrency = currency
            )
        }
        _state.update { state ->
            if (state.mode == CalcMode.BY_LOTS && result.isValid) {
                state.copy(
                    result = result,
                    riskAmount = "%.2f".format(result.riskAmount),
                    riskPercent = "%.2f".format(result.riskPercent)
                )
            } else {
                state.copy(result = result)
            }
        }
    }
}
