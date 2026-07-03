package com.securechat.phoenix.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// WhatsApp-inspired color palette
object ChatColors {
    // Primary teal/green
    val Teal = Color(0xFF075E54)
    val TealDark = Color(0xFF054D44)
    val TealLight = Color(0xFF128C7E)
    val Green = Color(0xFF25D366)
    val GreenLight = Color(0xFFDCF8C6)

    // Chat background
    val ChatBgLight = Color(0xFFECE5DD)
    val ChatBgDark = Color(0xFF0B141A)

    // Bubbles
    val BubbleOutLight = Color(0xFFD9FDD3)
    val BubbleInLight = Color(0xFFFFFFFF)
    val BubbleOutDark = Color(0xFF005C4B)
    val BubbleInDark = Color(0xFF1F2C34)

    // Status ticks
    val TickGray = Color(0xFF8696A0)
    val TickBlue = Color(0xFF53BDEB)

    // Text
    val TextPrimary = Color(0xFF111B21)
    val TextSecondary = Color(0xFF667781)
    val TextPrimaryDark = Color(0xFFE9EDEF)
    val TextSecondaryDark = Color(0xFF8696A0)

    // Surface
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF111B21)
    val AppBarDark = Color(0xFF1F2C34)

    // Unread badge
    val UnreadBadge = Color(0xFF25D366)

    // Divider
    val DividerLight = Color(0xFFE9EDEF)
    val DividerDark = Color(0xFF222D34)

    // Online indicator
    val OnlineGreen = Color(0xFF25D366)
}

private val DarkColorScheme = darkColorScheme(
    primary = ChatColors.TealLight,
    onPrimary = Color.White,
    secondary = ChatColors.Green,
    onSecondary = Color.White,
    tertiary = ChatColors.TealLight,
    background = ChatColors.SurfaceDark,
    surface = ChatColors.AppBarDark,
    onBackground = ChatColors.TextPrimaryDark,
    onSurface = ChatColors.TextPrimaryDark,
    surfaceVariant = ChatColors.BubbleInDark,
    outline = ChatColors.DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = ChatColors.Teal,
    onPrimary = Color.White,
    secondary = ChatColors.Green,
    onSecondary = Color.White,
    tertiary = ChatColors.TealLight,
    background = ChatColors.SurfaceLight,
    surface = ChatColors.SurfaceLight,
    onBackground = ChatColors.TextPrimary,
    onSurface = ChatColors.TextPrimary,
    surfaceVariant = ChatColors.ChatBgLight,
    outline = ChatColors.DividerLight
)

@Composable
fun PhoenixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                ChatColors.AppBarDark.toArgb()
            } else {
                ChatColors.Teal.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
