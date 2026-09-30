package com.thrustwealth.app.ui.calculator

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.thrustwealth.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.salesmartly.chatwidget.domain.model.channelList
import com.thrustwealth.app.data.model.CalcMode
import com.thrustwealth.app.engine.InstrumentConfig
import com.thrustwealth.app.ui.components.ChatFloatingActionButton
import com.thrustwealth.app.ui.components.ChatFloatingSpeedDial
import com.thrustwealth.app.ui.components.enabledSupportChannels
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
    onOpenChat: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val unreadCount by viewModel.unreadChatCount.collectAsState()
    val salesmartlyReady by viewModel.salesmartlyReady.collectAsState()
    val salesmartlyUiState by viewModel.salesmartlyUiState.collectAsState()
    val widgetInfo = salesmartlyUiState
        ?.takeIf { salesmartlyReady && !it.isWidgetInfoLoading }
        ?.widgetInfo
    val brandColor = remember(widgetInfo?.background_color) {
        parseHexColor(widgetInfo?.background_color, fallback = Color(0xFFE9AE17))
    }
    val channels = remember(widgetInfo) {
        widgetInfo?.let { enabledSupportChannels(it.channelList(), it.channel) }.orEmpty()
    }

    val colors = ThrustTheme.colors
    val context = LocalContext.current
    val instrument = viewModel.currentInstrument()
    val currency = state.preferences.accountCurrency

    val fabBottomPadding by animateDpAsState(
        targetValue = if (state.searchQuery.isBlank()) 68.dp else 20.dp,
        label = "chat_fab_bottom_padding"
    )

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Bar: 统一品牌 App Icon + 标题及动作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(com.thrustwealth.app.R.drawable.ic_thrust_text_logo),
                    contentDescription = "Thrust Wealth",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(26.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
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

        // 紧凑排版的计算器滚动区域: 保证首屏露出结果卡与主数值
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            SearchField(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                placeholder = "Search tools (P/L, Specs, Journal)...",
                isLoading = state.isSearchingJump,
                onSearch = { query ->
                    viewModel.searchKeyword(query)
                }
            )

            if (state.searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SearchResults(
                    query = state.searchQuery,
                    onSearch = { query ->
                        viewModel.searchKeyword(query)
                    },
                    onPnl = {
                        viewModel.trackSearchJump("pnl")
                        onOpenPnl()
                    },
                    onSpecs = {
                        viewModel.trackSearchJump("specs")
                        onOpenSpecs()
                    },
                    onJournal = {
                        viewModel.trackSearchJump("journal")
                        onOpenJournal()
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedControl(
                    options = listOf("By Risk", "By Lots"),
                    selectedIndex = if (state.mode == CalcMode.BY_RISK) 0 else 1,
                    onSelect = { viewModel.setMode(if (it == 0) CalcMode.BY_RISK else CalcMode.BY_LOTS) },
                    height = 34.dp
                )

                Spacer(modifier = Modifier.height(8.dp))
                FormGroup(
                    label = "Instrument / Symbol",
                    hint = instrument?.let { "Contract: ${it.contractSize.toInt()} ${it.unit} / lot" }
                ) {
                    SymbolDropdownButton(
                        symbol = state.symbol,
                        onClick = { viewModel.showSymbolPicker(true) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                FormGroup(label = "Account Balance", hint = "Base Currency: $currency") {
                    FormInput(
                        value = state.balance,
                        onValueChange = viewModel::setBalance,
                        prefix = FormatUtils.currencySymbol(currency)
                    )
                }

                if (state.mode == CalcMode.BY_RISK) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5, 1.0, 2.0, 3.0).forEach { preset ->
                            val active = state.riskPercent.toDoubleOrNull() == preset
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (active) colors.brand else colors.bgInput,
                                        RoundedCornerShape(50)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) colors.brand else colors.borderMedium,
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { viewModel.setPresetRisk(preset) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${preset}%",
                                    color = if (active) Color.White else colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    FormGroup(label = "Lot Size", hint = "Standard Lots") {
                        FormInput(
                            value = state.lotSize,
                            onValueChange = viewModel::setLotSize,
                            suffix = "lots"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Spacer(modifier = Modifier.height(10.dp))
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

                Spacer(modifier = Modifier.height(10.dp))
                ComplianceDisclaimer()
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // 快捷品种栏: 固定停靠在底部导航栏上方, 绝对不遮盖上方滚动的卡片
        if (state.searchQuery.isBlank()) {
            QuickSymbolBar(
                symbols = InstrumentConfig.QUICK_SYMBOLS,
                selected = state.symbol,
                onSelect = viewModel::setSymbol
            )
        }
    }

    // 首页客服多渠道悬浮挂件（固定显示 SaleSmartly、WhatsApp、Telegram 三个图标）
    // 确保 SDK 和配置（widgetInfo）完全加载后再显示入口，避免 "is not configured yet" 错误
    AnimatedVisibility(
        visible = widgetInfo != null,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = fabBottomPadding),
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        ChatFloatingSpeedDial(
            unreadCount = unreadCount,
            onChatClick = {
                viewModel.prepareNativeChat()
                onOpenChat()
            },
            onExternalChannelClick = { channel ->
                val targetUrl = viewModel.externalChannelUrl(channel)
                if (!targetUrl.isNullOrBlank()) {
                    runCatching {
                        val uri = Uri.parse(targetUrl)
                        val intent = if (targetUrl.startsWith("mailto:", ignoreCase = true)) {
                            Intent(Intent.ACTION_SENDTO, uri)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "Could not open $channel application", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "$channel is not configured yet", Toast.LENGTH_SHORT).show()
                }
            }
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
                SheetAction("Customer Support") {
                    onOpenChat()
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
    onSearch: (String) -> Unit,
    onPnl: () -> Unit,
    onSpecs: () -> Unit,
    onJournal: () -> Unit
) {
    val colors = ThrustTheme.colors
    val trimmed = query.trim()
    val tools = listOf(
        Triple("P/L Calculator", "Profit / loss by open & close", onPnl),
        Triple("Market Specs", "Contract size & pip value", onSpecs),
        Triple("Trade Journal", "Local trade log & stats", onJournal)
    ).filter {
        it.first.contains(query, true) || it.second.contains(query, true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Direct search option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgElevated, RoundedCornerShape(12.dp))
                .clickable { onSearch(trimmed) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search \"$trimmed\"",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Query remote service & matching features",
                    color = colors.textTertiary,
                    fontSize = 12.sp
                )
            }
        }

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

private fun parseHexColor(hex: String?, fallback: Color = Color(0xFFE9AE17)): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val clean = hex.trim()
        val formatted = if (clean.startsWith("#")) clean else "#$clean"
        Color(android.graphics.Color.parseColor(formatted))
    }.getOrDefault(fallback)
}
