package com.thrustwealth.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.R
import com.thrustwealth.app.ui.theme.ThrustTheme

/**
 * 悬浮客服多渠道展开收起挂件（参考 SaleSmartly Chat Widget 交互规范）。
 *
 * 1. 响应式绑定后台配置的品牌色 [brandColor] 与渠道列表 [channels]；
 * 2. 主按钮与展开子按钮均保持 56dp 统一尺寸；
 * 3. 未展开时主按钮为加号“+”，展开后旋转平滑切换为叉号“×”；
 * 4. 移除多余文字气泡，保持纯净利落的圆形操作体验；
 * 5. WhatsApp、Chat 等外部渠道均采用精美矢量官方图标。
 */
@Composable
fun ChatFloatingSpeedDial(
    unreadCount: Int,
    onChatClick: () -> Unit,
    onExternalChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val badgeRed = Color(0xFFEF4444)

        // 1. SaleSmartly Chat Bubble
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_bubble_salesmartly),
                contentDescription = "Customer Support",
                modifier = Modifier
                    .size(76.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onChatClick
                    )
            )

            // Unread badge on top right
            if (unreadCount > 0) {
                val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeRed)
                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // 2. WhatsApp Bubble
        Image(
            painter = painterResource(id = R.drawable.ic_bubble_whatsapp),
            contentDescription = "WhatsApp",
            modifier = Modifier
                .size(76.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onExternalChannelClick("whatsapp") }
                )
        )

        // 3. Telegram Bubble
        Image(
            painter = painterResource(id = R.drawable.ic_bubble_telegram),
            contentDescription = "Telegram",
            modifier = Modifier
                .size(76.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onExternalChannelClick("telegram") }
                )
        )
    }
}

/**
 * 主悬浮按钮
 */
@Composable
private fun MainFloatingButton(
    isExpanded: Boolean,
    rotationAngle: Float,
    brandColor: Color,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "fab_press_scale"
    )

    val buttonBg = if (isExpanded) Color(0xFF333333) else brandColor
    val badgeRed = Color(0xFFEF4444)

    Box(
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (unreadCount > 0) {
                    "Customer Support, $unreadCount unread messages"
                } else {
                    "Customer Support"
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(buttonBg, buttonBg.copy(alpha = 0.9f))
                    )
                )
                .border(
                    width = 1.2.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.rotate(rotationAngle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close" else "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 右上角未读消息角标（仅收起时展示）
        AnimatedVisibility(
            visible = unreadCount > 0 && !isExpanded,
            enter = scaleIn(animationSpec = spring(stiffness = 500f)),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
        ) {
            val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeRed)
                    .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

/**
 * 展开状态下的渠道子菜单项（与主按钮保持一致的 56dp 规格，移除文字气泡）
 */
@Composable
private fun ChannelSpeedDialItem(
    channel: String,
    brandColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = resolveChannelMetadata(channel, defaultBrandColor = brandColor)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "channel_fab_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(meta.brandColor)
            .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        ChannelIcon(
            channel = channel,
            meta = meta,
            iconSize = 28.dp
        )
    }
}

/**
 * 统一的渠道图标渲染器
 */
@Composable
fun ChannelIcon(
    channel: String,
    meta: ChannelMetadata,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp
) {
    when (channel.lowercase()) {
        "whatsapp" -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_whatsapp),
                contentDescription = meta.title,
                tint = Color.White,
                modifier = modifier.size(iconSize)
            )
        }
        "chat" -> {
            Box(modifier = modifier.semantics { 
                contentDescription = meta.title 
            }) {
                ChatBubbleWithDotsIcon(
                    modifier = Modifier.size(iconSize),
                    bubbleColor = Color.White,
                    dotsColor = meta.brandColor
                )
            }
        }
        else -> {
            val sdkIcon = when (channel.lowercase()) {
                "telegram" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_telegram
                "messenger" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_messenger
                "line" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_line
                "lineapp" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_line_app
                "email" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_email
                "instagram" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_instagram
                "tiktok" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_tiktok
                "zalo" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_zalo
                "vkontakte" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_vkontakte
                "weixin" -> com.salesmartly.chatwidget.R.drawable.ic_ssc_channel_weixin
                else -> null
            }
            if (sdkIcon != null) {
                Icon(
                    painter = painterResource(id = sdkIcon),
                    contentDescription = meta.title,
                    tint = Color.White,
                    modifier = modifier.size(iconSize)
                )
            } else {
                Text(
                    text = meta.badgeLetter,
                    color = Color.White,
                    fontSize = if (meta.badgeLetter.length > 1) 13.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = modifier.semantics { contentDescription = meta.title }
                )
            }
        }
    }
}

/**
 * 渠道元数据（标题、字母/标志、品牌主色）
 */
data class ChannelMetadata(
    val title: String,
    val badgeLetter: String,
    val brandColor: Color
)

/** Keep the SDK's preferred order, then append newly enabled channels it does not yet sort. */
fun enabledSupportChannels(ordered: List<String>, configured: List<String>): List<String> {
    val enabled = configured.map(String::trim).filter(String::isNotEmpty).distinct()
    return (ordered.filter { it in enabled } + enabled).distinct()
}

fun resolveChannelMetadata(channel: String, defaultBrandColor: Color = Color(0xFFE9AE17)): ChannelMetadata {
    return when (channel.lowercase()) {
        "chat" -> ChannelMetadata("Chat", "C", defaultBrandColor)
        "whatsapp" -> ChannelMetadata("WhatsApp", "W", Color(0xFF25D366))
        "telegram" -> ChannelMetadata("Telegram", "T", Color(0xFF229ED9))
        "messenger" -> ChannelMetadata("Messenger", "M", Color(0xFF0084FF))
        "line", "lineapp" -> ChannelMetadata("Line", "L", Color(0xFF06C755))
        "email" -> ChannelMetadata("Email", "E", Color(0xFFEA4335))
        "instagram" -> ChannelMetadata("Instagram", "I", Color(0xFFE1306C))
        "tiktok" -> ChannelMetadata("TikTok", "TK", Color(0xFF000000))
        "zalo" -> ChannelMetadata("Zalo", "Z", Color(0xFF0068FF))
        "vkontakte" -> ChannelMetadata("VKontakte", "VK", Color(0xFF0077FF))
        "weixin" -> ChannelMetadata("WeChat", "W", Color(0xFF07C160))
        else -> ChannelMetadata(
            title = channel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            badgeLetter = channel.take(1).uppercase(),
            brandColor = Color(0xFF4A90E2)
        )
    }
}

/**
 * 悬浮客服聊天入口组件（基础按钮兼容版本）
 */
@Composable
fun ChatFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0
) {
    MainFloatingButton(
        isExpanded = false,
        rotationAngle = 0f,
        brandColor = Color(0xFFE9AE17),
        unreadCount = unreadCount,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * 绘制高质感圆角聊天气泡与内部三点图标
 */
@Composable
private fun ChatBubbleWithDotsIcon(
    modifier: Modifier = Modifier,
    bubbleColor: Color = Color.White,
    dotsColor: Color = Color(0xFF2E7CF6)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. 绘制主体气泡（圆角矩形）
        val bubbleLeft = w * 0.08f
        val bubbleTop = h * 0.12f
        val bubbleRight = w * 0.92f
        val bubbleBottom = h * 0.72f
        val bubbleRadius = (bubbleBottom - bubbleTop) * 0.44f

        val bubblePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bubbleLeft,
                    top = bubbleTop,
                    right = bubbleRight,
                    bottom = bubbleBottom,
                    cornerRadius = CornerRadius(bubbleRadius, bubbleRadius)
                )
            )
            // 2. 绘制左下方指向尾巴
            moveTo(w * 0.22f, bubbleBottom - 1.dp.toPx())
            lineTo(w * 0.12f, h * 0.90f)
            lineTo(w * 0.40f, bubbleBottom - 1.dp.toPx())
            close()
        }

        drawPath(
            path = bubblePath,
            color = bubbleColor
        )

        // 3. 绘制气泡中间水平排列的 3 个小圆点
        val centerY = (bubbleTop + bubbleBottom) / 2f
        val dotRadius = w * 0.045f
        val dotSpacing = w * 0.15f

        val centerDotX = (bubbleLeft + bubbleRight) / 2f
        val leftDotX = centerDotX - dotSpacing
        val rightDotX = centerDotX + dotSpacing

        drawCircle(
            color = dotsColor,
            radius = dotRadius,
            center = Offset(leftDotX, centerY)
        )
        drawCircle(
            color = dotsColor,
            radius = dotRadius,
            center = Offset(centerDotX, centerY)
        )
        drawCircle(
            color = dotsColor,
            radius = dotRadius,
            center = Offset(rightDotX, centerY)
        )
    }
}
