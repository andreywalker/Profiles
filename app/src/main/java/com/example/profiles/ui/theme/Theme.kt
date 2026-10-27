package com.example.profiles.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark, onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenContainerDark, onPrimaryContainer = GreenOnContainerDark,
    background = BackgroundDark, surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark, onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColors = lightColorScheme(
    primary = GreenPrimaryLight, onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenContainerLight, onPrimaryContainer = GreenOnContainerLight,
    background = BackgroundLight, surface = BackgroundLight,
    surfaceVariant = SurfaceVariantLight, onSurface = OnSurfaceLight
)

@Composable
fun ProfilesTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}