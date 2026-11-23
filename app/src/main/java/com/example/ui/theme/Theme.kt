package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = StudioDarkPrimary,
  onPrimary = Color(0xFF471808),
  primaryContainer = StudioDarkPrimaryContainer,
  onPrimaryContainer = StudioTerracottaContainer,
  secondary = StudioDarkSecondary,
  onSecondary = Color(0xFF102A1C),
  secondaryContainer = StudioDarkSecondaryContainer,
  onSecondaryContainer = StudioSageContainer,
  background = StudioDarkBackground,
  onBackground = StudioDarkOnSurface,
  surface = StudioDarkSurface,
  onSurface = StudioDarkOnSurface,
  surfaceVariant = StudioDarkSurfaceVariant,
  onSurfaceVariant = StudioDarkOnSurfaceVariant,
  outline = StudioDarkOutline,
  outlineVariant = Color(0xFF333942),
)

private val LightColorScheme = lightColorScheme(
  primary = StudioTerracotta,
  onPrimary = Color.White,
  primaryContainer = StudioTerracottaContainer,
  onPrimaryContainer = StudioOnTerracottaContainer,
  secondary = StudioSage,
  onSecondary = Color.White,
  secondaryContainer = StudioSageContainer,
  onSecondaryContainer = StudioOnSageContainer,
  background = StudioAlabaster,
  onBackground = StudioOnSurface,
  surface = StudioSurface,
  onSurface = StudioOnSurface,
  surfaceVariant = StudioSurfaceVariant,
  onSurfaceVariant = StudioOnSurfaceVariant,
  outline = StudioOutline,
  outlineVariant = StudioOutlineVariant,
)

@Composable
fun RoomCraftTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    content = content,
  )
}
