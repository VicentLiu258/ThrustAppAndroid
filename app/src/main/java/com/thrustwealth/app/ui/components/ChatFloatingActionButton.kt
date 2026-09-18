package com.thrustwealth.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
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

/**
 * 悬浮客服聊天入口组件（参考 SaleSmartly 悬浮气泡入口与设计规范）。
 *
 * @param onClick 点击事件回调
 * @param unreadCount 未读消息数，> 0 时展示右上角红色角标
 * @param modifier 外部修饰符
 */
@Composable
fun ChatFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "fab_press_scale"
    )

    val primaryBlue = Color(0xFF2E7CF6)
    val darkBlue = Color(0xFF1E66DE)
    val badgeRed = Color(0xFFEF4444)

    Box(
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (unreadCount > 0) {
                    "Customer Support Chat, $unreadCount unread messages"
                } else {
                    "Customer Support Chat"
                }
            }
    ) {
        // 主体圆形按钮
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryBlue, darkBlue)
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // 聊天气泡及内部三点图标
            ChatBubbleWithDotsIcon(
                modifier = Modifier.size(28.dp),
                bubbleColor = Color.White,
                dotsColor = primaryBlue
            )
        }

        // 右上角未读消息角标
        AnimatedVisibility(
            visible = unreadCount > 0,
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

        // 3. 绘制气泡中间水平排列的 3 个蓝色小圆点
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
