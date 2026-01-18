package com.example.myapplication.ui.theme
import androidx.compose.ui.graphics.Color

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SoftOrange,
    background = WarmCream,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = DeepCharcoal
)

private val DarkColorScheme = darkColorScheme(
    primary = MutedAmber,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = WarmCream
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Custom soft typography
        content = content
    )
}