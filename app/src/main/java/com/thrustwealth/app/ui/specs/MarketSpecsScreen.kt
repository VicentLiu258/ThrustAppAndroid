package com.thrustwealth.app.ui.specs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.repository.InstrumentRepository
import com.thrustwealth.app.engine.CalculationEngine
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.components.FormGroup
import com.thrustwealth.app.ui.components.FormInput
import com.thrustwealth.app.ui.components.SearchField
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

data class SpecsUiState(
    val query: String = "",
    val category: String = "ALL",
    val detail: Instrument? = null,
    val showAdd: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class MarketSpecsViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(SpecsUiState())
    val ui = _ui.asStateFlow()

    val instruments = instrumentRepository.observeInstruments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { instrumentRepository.seedIfNeeded() }
    }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun setCategory(c: String) = _ui.update { it.copy(category = c) }
    fun showDetail(i: Instrument?) = _ui.update { it.copy(detail = i) }
    fun showAdd(v: Boolean) = _ui.update { it.copy(showAdd = v) }
    fun clearMessage() = _ui.update { it.copy(message = null) }

    fun toggleFavorite(instrument: Instrument) {
        viewModelScope.launch {
            instrumentRepository.setFavorite(instrument.id, !instrument.isFavorite)
        }
    }

    fun addCustom(symbol: String, name: String, contract: Double, pip: Double) {
        viewModelScope.launch {
            instrumentRepository.upsert(
                Instrument(
                    id = symbol.uppercase(),
                    name = name,
                    category = "Custom",
                    contractSize = contract,
                    unit = "units",
                    pipSize = pip,
                    tickSize = pip / 10,
                    digits = 2,
                    baseCurrency = "USD",
                    quoteCurrency = "USD",
                    defaultPrice = 1.0,
                    isCustom = true
                )
            )
            _ui.update { it.copy(showAdd = false, message = "Custom instrument saved") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSpecsScreen(
    onBack: () -> Unit,
    viewModel: MarketSpecsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val instruments by viewModel.instruments.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current

    LaunchedEffect(ui.message) {
        ui.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val filtered = remember(instruments, ui.query, ui.category) {
        instruments.filter { inst ->
            val matchCat = when (ui.category) {
                "ALL" -> true
                "Metals" -> inst.category == "Metals"
                "Forex" -> inst.category == "Forex"
                "Indices" -> inst.category.contains("Indices") || inst.category == "Custom"
                else -> true
            }
            val matchQuery = ui.query.isBlank() ||
                inst.id.contains(ui.query, true) ||
                inst.name.contains(ui.query, true)
            matchCat && matchQuery
        }
    }
    val favorites = filtered.filter { it.isFavorite }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(
            title = "Market Specs",
            onBack = onBack,
            actions = {
                IconButton(onClick = { ExportHelper.exportSpecsCsv(context, instruments) }) {
                    Icon(Icons.Outlined.FileDownload, "Export", tint = colors.textPrimary)
                }
                IconButton(onClick = { viewModel.showAdd(true) }) {
                    Icon(Icons.Outlined.Add, "Add", tint = colors.textPrimary)
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SearchField(query = ui.query, onQueryChange = viewModel::setQuery, placeholder = "Search symbol or name...")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All Instruments", "Metals" to "Precious Metals", "Forex" to "Forex", "Indices" to "Indices & Energy")
                    .forEach { (key, label) ->
                        val active = ui.category == key
                        Box(
                            modifier = Modifier
                                .background(if (active) colors.brand else colors.bgInput, RoundedCornerShape(20.dp))
                                .border(1.dp, if (active) colors.brand else colors.borderMedium, RoundedCornerShape(20.dp))
                                .clickable { viewModel.setCategory(key) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(label, color = if (active) Color.White else colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (favorites.isNotEmpty() && ui.category == "ALL" && ui.query.isBlank()) {
                item {
                    Text("★ FAVORITES", color = colors.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                }
                items(favorites, key = { "fav-${it.id}" }) { inst ->
                    SpecRow(inst, onClick = { viewModel.showDetail(inst) }, onToggleFav = { viewModel.toggleFavorite(inst) })
                }
                item {
                    Text("ALL", color = colors.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
            val groups = filtered.groupBy { it.category }
            groups.forEach { (cat, list) ->
                item {
                    Text(cat.uppercase(), color = colors.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                }
                items(list, key = { it.id }) { inst ->
                    SpecRow(inst, onClick = { viewModel.showDetail(inst) }, onToggleFav = { viewModel.toggleFavorite(inst) })
                }
            }
            item {
                Text(
                    "Tap any instrument to view full contract specifications. Reference only — actual specs vary by broker.",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                )
            }
        }
    }

    val detailInstrument = ui.detail
    if (detailInstrument != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDetail(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.bgSurface
        ) {
            val pipValue = CalculationEngine.calculatePipValue(detailInstrument)
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(detailInstrument.id, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { viewModel.showDetail(null) }) {
                        Icon(Icons.Outlined.Close, null, tint = colors.textSecondary)
                    }
                }
                Text(detailInstrument.name, color = colors.textSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                SpecDetailRow("Category", detailInstrument.category)
                SpecDetailRow("Contract Size", "${detailInstrument.contractSize} ${detailInstrument.unit} / lot")
                SpecDetailRow("Pip Size", detailInstrument.pipSize.toString())
                SpecDetailRow("Tick Size", detailInstrument.tickSize.toString())
                SpecDetailRow("Digits", detailInstrument.digits.toString())
                SpecDetailRow("Pip Value (1 lot)", FormatUtils.formatMoney(pipValue))
                SpecDetailRow("Default Price", FormatUtils.formatPrice(detailInstrument.defaultPrice, detailInstrument.digits))
                SpecDetailRow("Lot Conversions", "1 Std = 10 Mini = 100 Micro")
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (ui.showAdd) {
        var symbol by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var contract by remember { mutableStateOf("1") }
        var pip by remember { mutableStateOf("1.0") }
        ModalBottomSheet(
            onDismissRequest = { viewModel.showAdd(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.bgSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create Custom Instrument", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                FormGroup(label = "Symbol") {
                    FormInput(value = symbol, onValueChange = { symbol = it.uppercase() }, placeholder = "e.g. BTCUSD", keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                }
                Spacer(modifier = Modifier.height(10.dp))
                FormGroup(label = "Display Name") {
                    FormInput(value = name, onValueChange = { name = it }, placeholder = "e.g. Bitcoin vs USD", keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormGroup(label = "Contract Size", rootModifier = Modifier.weight(1f)) {
                        FormInput(value = contract, onValueChange = { contract = it })
                    }
                    FormGroup(label = "Pip Size", rootModifier = Modifier.weight(1f)) {
                        FormInput(value = pip, onValueChange = { pip = it })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(colors.brand, RoundedCornerShape(12.dp))
                        .clickable {
                            if (symbol.isNotBlank() && name.isNotBlank()) {
                                viewModel.addCustom(
                                    symbol,
                                    name,
                                    contract.toDoubleOrNull() ?: 1.0,
                                    pip.toDoubleOrNull() ?: 1.0
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Custom Spec", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SpecRow(inst: Instrument, onClick: () -> Unit, onToggleFav: () -> Unit) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgElevated, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(inst.id, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${inst.name} · ${inst.contractSize.toInt()} ${inst.unit} / lot", color = colors.textTertiary, fontSize = 12.sp)
        }
        IconButton(onClick = onToggleFav, modifier = Modifier.size(36.dp)) {
            Icon(
                if (inst.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorite",
                tint = if (inst.isFavorite) Color(0xFFEAB308) else colors.textTertiary
            )
        }
    }
}

@Composable
private fun SpecDetailRow(label: String, value: String) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.textTertiary, fontSize = 13.sp)
        Text(value, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
