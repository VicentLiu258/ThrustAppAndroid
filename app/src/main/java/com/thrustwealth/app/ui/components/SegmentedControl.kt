package com.thrustwealth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 36.dp,
    selectedColor: Color? = null,
    selectedContentColor: Color = Color.White
) {
    val colors = ThrustTheme.colors
    val activeBg = selectedColor ?: colors.brand

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgInput)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(2.5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(height - 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) activeBg else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = when {
                        selected -> selectedContentColor
                        else -> colors.textSecondary
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
fun DirectionSegmented(
    isLong: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    SegmentedControl(
        options = listOf("Long", "Short"),
        selectedIndex = if (isLong) 0 else 1,
        onSelect = { onChange(it == 0) },
        modifier = modifier,
        height = 44.dp,
        selectedColor = if (isLong) colors.profit else colors.loss
    )
}
