package com.thrustwealth.app.ui.tools

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.components.AppTopBar
import com.thrustwealth.app.ui.components.ComplianceDisclaimer
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun ToolsScreen(
    onOpenPnl: () -> Unit,
    onOpenSpecs: () -> Unit
) {
    val colors = ThrustTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp)
    ) {
        AppTopBar(title = "Tools")
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolCard(
                icon = Icons.Outlined.AttachMoney,
                title = "P/L Calculator",
                description = "Calculate profit, loss, price delta, and return on notional by open and close prices.",
                onClick = onOpenPnl
            )
            ToolCard(
                icon = Icons.Outlined.GridView,
                title = "Market Specs",
                description = "Search and inspect contract sizes, tick sizes, and pip values across all instruments.",
                onClick = onOpenSpecs
            )
            ToolCard(
                icon = Icons.Outlined.Calculate,
                title = "Required Margin (V2.0)",
                description = "Determine exact required margin and free margin leverage requirements.",
                enabled = false,
                onClick = null
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComplianceDisclaimer(
                text = "For calculation purposes only. Not investment advice. Always manage trade risk prudently."
            )
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = ThrustTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .background(colors.bgSurface, shape)
            .border(1.dp, colors.borderSubtle, shape)
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (enabled) colors.brand.copy(alpha = 0.12f) else colors.bgInput,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) colors.brand else colors.textTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = colors.textTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary
            )
        }
    }
}
