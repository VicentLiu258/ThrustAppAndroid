package com.thrustwealth.app.ui.history

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.repository.HistoryRepository
import com.thrustwealth.app.ui.components.AppTopBar
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
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val records = historyRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showClear = MutableStateFlow(false)
    val showClear = _showClear.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun showClear(v: Boolean) = _showClear.update { v }
    fun clearMessage() = _message.update { null }

    fun delete(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _showClear.update { false }
            _message.update { "History cleared" }
        }
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onRestore: (CalculationRecord) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsState()
    val showClear by viewModel.showClear.collectAsState()
    val message by viewModel.message.collectAsState()
    val colors = ThrustTheme.colors
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val grouped = remember(records) {
        records.groupBy { FormatUtils.formatDateGroup(it.createdAt) }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgApp)) {
        AppTopBar(
            title = "Calculation History",
            onBack = onBack,
            actions = {
                IconButton(onClick = { ExportHelper.exportHistoryCsv(context, records) }) {
                    Icon(Icons.Outlined.FileDownload, "Export", tint = colors.textPrimary)
                }
                IconButton(onClick = { viewModel.showClear(true) }) {
                    Icon(Icons.Outlined.DeleteOutline, "Clear", tint = colors.loss)
                }
            }
        )

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No calculation history yet.", color = colors.textTertiary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        "Automatically saved locally (last 50 calculations). Tap any record to restore its parameters.",
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                grouped.forEach { (group, list) ->
                    item {
                        Text(
                            group,
                            color = colors.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(list, key = { it.id }) { record ->
                        HistoryRow(
                            record = record,
                            onClick = {
                                onRestore(record)
                                onBack()
                            },
                            onDelete = { viewModel.delete(record.id) }
                        )
                    }
                }
            }
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { viewModel.showClear(false) },
            title = { Text("Clear History?") },
            text = { Text("This will permanently delete all saved calculations.") },
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
private fun HistoryRow(
    record: CalculationRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
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
            Text(
                "${FormatUtils.formatTime(record.createdAt)}  ${record.symbol}  Risk ${FormatUtils.formatPercent(record.riskPercent)} · SL ${record.stopPips.toInt()} pips",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "→ ${FormatUtils.formatLots(record.lotSize)} lots",
                color = colors.brandLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.DeleteOutline, "Delete", tint = colors.textMuted, modifier = Modifier.size(16.dp))
        }
    }
}
