package com.thrustwealth.app.ui.journal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thrustwealth.app.data.model.JournalStats
import com.thrustwealth.app.data.model.TradeEntry
import com.thrustwealth.app.data.repository.PreferencesRepository
import com.thrustwealth.app.data.repository.TradeRepository
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.components.ComplianceDisclaimer
import com.thrustwealth.app.ui.components.DirectionSegmented
import com.thrustwealth.app.ui.components.FormGroup
import com.thrustwealth.app.ui.components.FormInput
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.util.ExportHelper
import com.thrustwealth.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val tradeRepository: TradeRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val trades = tradeRepository.observeTrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stats = tradeRepository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalStats())
    val currency = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.thrustwealth.app.data.model.AppPreferences())

    private val _showAdd = MutableStateFlow(false)
    val showAdd = _showAdd.asStateFlow()
    private val _showClear = MutableStateFlow(false)
    val showClear = _showClear.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun showAdd(v: Boolean) = _showAdd.update { v }
    fun showClear(v: Boolean) = _showClear.update { v }
    fun clearMessage() = _message.update { null }

    fun addTrade(
        symbol: String,
        isLong: Boolean,
        lots: Double,
        open: Double,
        close: Double,
        pnl: Double,
        note: String
    ) {
        viewModelScope.launch {
            tradeRepository.insert(
                TradeEntry(
                    symbol = symbol.uppercase(),
                    direction = if (isLong) "long" else "short",
                    lots = lots,
                    entryPrice = open,
                    exitPrice = close,
                    pnl = pnl,
                    note = note
                )
            )
            _showAdd.update { false }
            _message.update { "Trade logged" }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { tradeRepository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch {
            tradeRepository.clearAll()
            _showClear.update { false }
            _message.update { "Journal cleared" }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel = hiltViewModel()) {
    val trades by viewModel.trades.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val prefs by viewModel.currency.collectAsState()
    val showAdd by viewModel.showAdd.collectAsState()
    val showClear by viewModel.showClear.collectAsState()
    val message by viewModel.message.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val currency = prefs.accountCurrency

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(
            title = "Trade Journal",
            actions = {
                IconButton(onClick = { viewModel.showAdd(true) }) {
                    Icon(Icons.Outlined.Add, "Add", tint = colors.textPrimary)
                }
                IconButton(onClick = { ExportHelper.exportTradesCsv(context, trades) }) {
                    Icon(Icons.Outlined.FileDownload, "Export", tint = colors.textPrimary)
                }
                IconButton(onClick = { viewModel.showClear(true) }) {
                    Icon(Icons.Outlined.DeleteOutline, "Clear", tint = colors.loss)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Net P/L", FormatUtils.formatSignedMoney(stats.netPnl, currency), stats.netPnl >= 0, Modifier.weight(1f))
                    StatCard("Win Rate", "${"%.0f".format(stats.winRate)}%", null, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Avg R:R Ratio", "%.1f".format(stats.avgRr), null, Modifier.weight(1f))
                    StatCard("Max Drawdown", FormatUtils.formatMoney(stats.maxDrawdown, currency), false, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Logged Trades", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Local Storage", color = colors.textTertiary, fontSize = 12.sp)
                }
            }
            if (trades.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trades yet. Tap + to log one.", color = colors.textTertiary)
                    }
                }
            } else {
                items(trades, key = { it.id }) { trade ->
                    TradeRow(trade, currency, onDelete = { viewModel.delete(trade.id) })
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ComplianceDisclaimer(text = "All trade logs are stored locally on this device. Export available in CSV.")
            }
        }
    }

    if (showAdd) {
        AddTradeSheet(
            onDismiss = { viewModel.showAdd(false) },
            onSave = { symbol, isLong, lots, open, close, pnl, note ->
                viewModel.addTrade(symbol, isLong, lots, open, close, pnl, note)
            }
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { viewModel.showClear(false) },
            title = { Text("Clear All Trades?") },
            text = { Text("This will permanently delete all journal entries on this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Clear", color = colors.loss)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClear(false) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, isProfit: Boolean?, modifier: Modifier = Modifier) {
    val colors = ThrustTheme.colors
    val valueColor = when (isProfit) {
        true -> colors.profit
        false -> colors.loss
        null -> colors.textPrimary
    }
    Column(
        modifier = modifier
            .background(colors.bgElevated, RoundedCornerShape(14.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(title, color = colors.textTertiary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TradeRow(trade: TradeEntry, currency: String, onDelete: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgElevated, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${FormatUtils.formatDate(trade.createdAt).take(6)}  ${trade.symbol}  ${trade.direction.replaceFirstChar { it.uppercase() }}  ${FormatUtils.formatLots(trade.lots)}",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (trade.note.isNotBlank()) {
                Text(trade.note, color = colors.textTertiary, fontSize = 11.sp)
            }
        }
        Text(
            FormatUtils.formatSignedMoney(trade.pnl, currency),
            color = if (trade.pnl >= 0) colors.profit else colors.loss,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.DeleteOutline, "Delete", tint = colors.textMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTradeSheet(
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Double, Double, Double, Double, String) -> Unit
) {
    val colors = ThrustTheme.colors
    var symbol by remember { mutableStateOf("XAUUSD") }
    var isLong by remember { mutableStateOf(true) }
    var lots by remember { mutableStateOf("0.20") }
    var open by remember { mutableStateOf("2350.00") }
    var close by remember { mutableStateOf("2360.00") }
    var pnl by remember { mutableStateOf("200.00") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bgSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Log New Trade", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(14.dp))
            FormGroup(label = "Symbol") {
                FormInput(value = symbol, onValueChange = { symbol = it.uppercase() }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroup(label = "Direction", rootModifier = Modifier.weight(1f)) {
                    DirectionSegmented(isLong = isLong, onChange = { isLong = it })
                }
                FormGroup(label = "Lots", rootModifier = Modifier.weight(1f)) {
                    FormInput(value = lots, onValueChange = { lots = it })
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormGroup(label = "Entry Price", rootModifier = Modifier.weight(1f)) {
                    FormInput(value = open, onValueChange = { open = it })
                }
                FormGroup(label = "Exit Price", rootModifier = Modifier.weight(1f)) {
                    FormInput(value = close, onValueChange = { close = it })
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            FormGroup(label = "Realized P/L") {
                FormInput(value = pnl, onValueChange = { pnl = it })
            }
            Spacer(modifier = Modifier.height(10.dp))
            FormGroup(label = "Trade Notes") {
                FormInput(value = note, onValueChange = { note = it }, placeholder = "e.g. Trend breakout on M15", keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(colors.brand, RoundedCornerShape(12.dp))
                    .clickable {
                        onSave(
                            symbol,
                            isLong,
                            lots.toDoubleOrNull() ?: 0.0,
                            open.toDoubleOrNull() ?: 0.0,
                            close.toDoubleOrNull() ?: 0.0,
                            pnl.toDoubleOrNull() ?: 0.0,
                            note
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Save to Journal", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
