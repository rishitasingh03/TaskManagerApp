package com.example.taskmanagerapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Primary palette
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_tertiary = Color(0xFF7D5260)
val md_theme_light_background = Color(0xFFF7F5FF)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_error = Color(0xFFB3261E)

val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_surface = Color(0xFF121212)
val md_theme_dark_error = Color(0xFFF2B8B5)

// Create light and dark color schemes for Material3
val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    tertiary = md_theme_light_tertiary,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    error = md_theme_light_error,
)

val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    error = md_theme_dark_error,
)