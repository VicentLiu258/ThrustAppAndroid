package com.thrustwealth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun ComplianceDisclaimer(
    text: String = "For calculation purposes only. Not investment advice, financial promotion, or a guarantee of returns.",
    modifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgInput, RoundedCornerShape(12.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun ResultShowcaseCard(
    badge: String,
    heroTitle: String,
    heroValue: String,
    heroSub: String? = null,
    heroValueColor: androidx.compose.ui.graphics.Color? = null,
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = ThrustTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgElevated, RoundedCornerShape(16.dp))
            .border(1.dp, colors.brand.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = badge.uppercase(),
                color = colors.brandLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Live Update",
                color = colors.textTertiary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = heroTitle, color = colors.textSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = heroValue,
            color = heroValueColor ?: colors.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        if (heroSub != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = heroSub, color = colors.textTertiary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderSubtle)
        )
        Spacer(modifier = Modifier.height(14.dp))

        val rows = stats.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, color = colors.textTertiary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = value,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(8.dp))
            actions()
        }
    }
}
