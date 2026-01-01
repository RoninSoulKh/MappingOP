package com.roninsoulkh.mappingop.ui.theme

import android.app.Activity
import android.os.Build
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

// Настройка для ТЕМНОЙ темы
private val DarkColorScheme = darkColorScheme(
    primary = CyanAction,
    onPrimary = Color.White,
    secondary = BlueAction,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    error = StatusRed,
    outline = DividerDark
)

// Настройка для СВЕТЛОЙ темы
private val LightColorScheme = lightColorScheme(
    primary = CyanAction,
    onPrimary = Color.White,
    secondary = BlueAction,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = StatusRed,
    outline = DividerLight
)

@Composable
fun MappingOPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Отключаем Dynamic Color, чтобы сохранить наш фирменный стиль (строгий дизайн)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Красим статус-бар в цвет фона
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // Если тема СВЕТЛАЯ -> иконки статус-бара должны быть ТЕМНЫМИ (и наоборот)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}