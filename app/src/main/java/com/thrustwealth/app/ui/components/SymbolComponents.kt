package com.thrustwealth.app.ui.components

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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun SymbolDropdownButton(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.bgInput, RoundedCornerShape(10.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = symbol,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.textTertiary
        )
    }
}

@Composable
fun QuickSymbolBar(
    symbols: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .border(width = 1.dp, color = colors.borderSubtle)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        symbols.forEach { symbol ->
            val active = symbol == selected
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
                    .clickable { onSelect(symbol) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = symbol,
                    color = if (active) androidx.compose.ui.graphics.Color.White else colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolPickerSheet(
    instruments: List<Instrument>,
    selectedId: String,
    onSelect: (Instrument) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ThrustTheme.colors
    var query by remember { mutableStateOf("") }
    val filtered = remember(instruments, query) {
        if (query.isBlank()) instruments
        else instruments.filter {
            it.id.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Instrument",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search symbol...")
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(
                            text = category.uppercase(),
                            color = colors.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items, key = { it.id }) { instrument ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (instrument.id == selectedId) colors.brand.copy(alpha = 0.12f)
                                    else colors.bgElevated,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelect(instrument)
                                    onDismiss()
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = instrument.id,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = instrument.name,
                                    color = colors.textTertiary,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    rootModifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSearch: ((String) -> Unit)? = null
) {
    val colors = ThrustTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused) colors.brand else colors.borderSubtle,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 500f),
        label = "search_border_color"
    )
    val borderWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 500f),
        label = "search_border_width"
    )
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused) colors.bgElevated else colors.bgInput,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 500f),
        label = "search_bg_color"
    )

    Row(
        modifier = rootModifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onSearch?.invoke(query) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = if (isFocused || query.isNotBlank()) colors.brand else colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch?.invoke(query) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = colors.textMuted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.brand
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
