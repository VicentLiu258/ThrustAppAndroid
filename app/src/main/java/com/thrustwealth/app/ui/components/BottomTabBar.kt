package com.thrustwealth.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.navigation.Screen
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun BottomTabBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    val tabs = listOf(
        Triple(Screen.Calculator, "Calculator", Icons.Outlined.Calculate),
        Triple(Screen.Tools, "Tools", Icons.Outlined.GridView),
        Triple(Screen.Journal, "Journal", Icons.Outlined.Book),
        Triple(Screen.Settings, "Settings", Icons.Outlined.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface.copy(alpha = 0.98f))
            .border(width = 1.dp, color = colors.borderSubtle)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (screen, label, icon) ->
                val selected = currentRoute == screen.route
                TabItem(
                    label = label,
                    icon = icon,
                    selected = selected,
                    onClick = { onNavigate(screen) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = ThrustTheme.colors
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 选中时 340ms Q弹放大动效，对应原型 @keyframes tabIconPop
    val targetScale = when {
        isPressed -> 0.90f
        selected -> 1.14f
        else -> 1.0f
    }
    val iconScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 340f),
        label = "tab_icon_scale"
    )

    val targetOffsetY = when {
        isPressed -> 1.dp
        selected -> (-2.5).dp
        else -> 0.dp
    }
    val iconOffsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 360f),
        label = "tab_icon_offset_y"
    )

    // 顶部品牌胶囊指示条展开动画 (宽 20dp, 高 3dp)
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "tab_indicator_width"
    )

    val activeColor by animateColorAsState(
        targetValue = if (selected) colors.brand else colors.textTertiary,
        animationSpec = spring(stiffness = 500f),
        label = "tab_active_color"
    )

    Box(
        modifier = Modifier
            .width(76.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // 顶部品牌指示微胶囊 (20px x 3px)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(indicatorWidth)
                .height(3.dp)
                .background(
                    if (indicatorWidth > 0.dp) colors.brand else Color.Transparent,
                    RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = iconOffsetY)
                    .scale(iconScale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = activeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = activeColor,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(com.thrustwealth.app.R.drawable.app_logo),
                contentDescription = "App Icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(7.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}
