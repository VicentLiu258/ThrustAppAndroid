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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.theme.ThrustTheme

/**
 * 悬浮客服多渠道展开收起挂件（参考 SaleSmartly Chat Widget 交互规范）。
 *
 * 1. 响应式绑定后台配置的品牌色 [brandColor] 与渠道列表 [channels]；
 * 2. 点击主按钮具有展开/收起平滑过渡动画（聊天气泡旋转切换为关闭图标）；
 * 3. 展开时在主按钮上方依次向上弹性弹出各个渠道快捷按钮；
 * 4. 点击“消息 / Chat”与“WhatsApp”分别触发对应原生会话或系统应用跳转。
 */
@Composable
fun ChatFloatingSpeedDial(
    channels: List<String>,
    brandColor: Color,
    unreadCount: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChatClick: () -> Unit,
    onExternalChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 主按钮旋转角度动画：0°（客服图标）-> 135°（变更为叉号）
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "fab_rotation"
    )

    val validChannels = remember(channels) {
        if (channels.isNotEmpty()) channels else listOf("chat", "whatsapp")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 向上展开的渠道项列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(140)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                validChannels.forEach { channel ->
                    ChannelSpeedDialItem(
                        channel = channel,
                        brandColor = brandColor,
                        onClick = {
                            onExpandedChange(false)
                            if (channel.lowercase() == "chat") {
                                onChatClick()
                            } else {
                                onExternalChannelClick(channel)
                            }
                        }
                    )
                }
            }
        }

        // 主悬浮按钮（FAB）
        MainFloatingButton(
            isExpanded = isExpanded,
            rotationAngle = rotationAngle,
            brandColor = brandColor,
            unreadCount = unreadCount,
            onClick = {
                if (validChannels.size <= 1 && validChannels.firstOrNull()?.lowercase() == "chat") {
                    onChatClick()
                } else {
                    onExpandedChange(!isExpanded)
                }
            }
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
                if (isExpanded) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    ChatBubbleWithDotsIcon(
                        modifier = Modifier.size(28.dp),
                        bubbleColor = Color.White,
                        dotsColor = buttonBg
                    )
                }
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
 * 展开状态下的渠道子菜单项（左侧文字胶囊标签 + 右侧品牌圆形按钮）
 */
@Composable
private fun ChannelSpeedDialItem(
    channel: String,
    brandColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = resolveChannelMetadata(channel, defaultBrandColor = brandColor)
    val colors = ThrustTheme.colors

    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // 左侧渠道标题标签胶囊
        Box(
            modifier = Modifier
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgElevated)
                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = meta.title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 右侧渠道品牌圆形图标
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(meta.brandColor)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (channel.lowercase() == "chat") {
                ChatBubbleWithDotsIcon(
                    modifier = Modifier.size(20.dp),
                    bubbleColor = Color.White,
                    dotsColor = meta.brandColor
                )
            } else {
                Text(
                    text = meta.badgeLetter,
                    color = Color.White,
                    fontSize = if (meta.badgeLetter.length > 1) 13.sp else 18.sp,
                    fontWeight = FontWeight.Bold
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
        brandColor = Color(0xFF2E7CF6),
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
