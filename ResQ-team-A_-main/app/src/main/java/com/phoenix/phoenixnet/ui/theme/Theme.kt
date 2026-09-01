package com.phoenix.phoenixnet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FireDarkColors = darkColorScheme(
    primary = Color(0xFFFF4500),
    secondary = Color(0xFFFF8C00),
    tertiary = Color(0xFFFFD700),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val FloodDarkColors = darkColorScheme(
    primary = Color(0xFF00CED1),
    secondary = Color(0xFF20B2AA),
    tertiary = Color(0xFF40E0D0),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun PhoenixTheme(
    isDarkMode: Boolean,
    isFireMode: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isDarkMode -> if (isFireMode) FireDarkColors else FloodDarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        // Use colorScheme background directly
        window.statusBarColor = android.graphics.Color.BLACK 
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
