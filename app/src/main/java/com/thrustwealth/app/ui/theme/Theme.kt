package com.thrustwealth.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThrustColors(
    val bgApp: Color,
    val bgSurface: Color,
    val bgElevated: Color,
    val bgHover: Color,
    val bgInput: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderMedium: Color,
    val brand: Color = BrandPrimary,
    val brandLight: Color = BrandLight,
    val profit: Color = Profit,
    val profitBg: Color = ProfitBg,
    val loss: Color = Loss,
    val lossBg: Color = LossBg,
    val warn: Color = Warn,
    val isDark: Boolean
)

val LocalThrustColors = staticCompositionLocalOf {
    ThrustColors(
        bgApp = DarkBgApp,
        bgSurface = DarkBgSurface,
        bgElevated = DarkBgElevated,
        bgHover = DarkBgHover,
        bgInput = DarkBgInput,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary,
        textMuted = DarkTextMuted,
        borderSubtle = DarkBorderSubtle,
        borderMedium = DarkBorderMedium,
        isDark = true
    )
}

private val DarkThrustColors = ThrustColors(
    bgApp = DarkBgApp,
    bgSurface = DarkBgSurface,
    bgElevated = DarkBgElevated,
    bgHover = DarkBgHover,
    bgInput = DarkBgInput,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    textMuted = DarkTextMuted,
    borderSubtle = DarkBorderSubtle,
    borderMedium = DarkBorderMedium,
    isDark = true
)

private val LightThrustColors = ThrustColors(
    bgApp = LightBgApp,
    bgSurface = LightBgSurface,
    bgElevated = LightBgElevated,
    bgHover = LightBgHover,
    bgInput = LightBgInput,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    textMuted = LightTextMuted,
    borderSubtle = LightBorderSubtle,
    borderMedium = LightBorderMedium,
    isDark = false
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandLight,
    background = DarkBgApp,
    surface = DarkBgSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = Loss
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandPrimaryHover,
    background = LightBgApp,
    surface = LightBgSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = Loss
)

@Composable
fun ThrustWealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val thrustColors = if (darkTheme) DarkThrustColors else LightThrustColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThrustColors provides thrustColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object ThrustTheme {
    val colors: ThrustColors
        @Composable
        get() = LocalThrustColors.current
}
