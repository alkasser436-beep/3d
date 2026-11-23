package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = StudioDarkPrimary,
  onPrimary = StudioDarkOnPrimary,
  primaryContainer = StudioDarkPrimaryContainer,
  onPrimaryContainer = StudioDarkOnPrimaryContainer,
  secondary = StudioDarkSecondary,
  onSecondary = StudioDarkOnSecondary,
  secondaryContainer = StudioDarkSecondaryContainer,
  onSecondaryContainer = StudioDarkOnSecondaryContainer,
  tertiary = StudioDarkTertiary,
  onTertiary = StudioDarkOnTertiary,
  tertiaryContainer = StudioDarkTertiaryContainer,
  onTertiaryContainer = StudioDarkOnTertiaryContainer,
  background = StudioDarkBackground,
  onBackground = StudioDarkOnSurface,
  surface = StudioDarkSurface,
  onSurface = StudioDarkOnSurface,
  surfaceVariant = StudioDarkSurfaceVariant,
  onSurfaceVariant = StudioDarkOnSurfaceVariant,
  surfaceContainerLowest = StudioDarkBackground,
  surfaceContainerLow = StudioDarkSurfaceLow,
  surfaceContainer = StudioDarkSurface,
  surfaceContainerHigh = StudioDarkSurfaceHigh,
  outline = StudioDarkOutline,
  outlineVariant = StudioDarkOutlineVariant,
  error = StudioDarkError,
  onError = Color(0xFF450A0A),
  errorContainer = StudioDarkErrorContainer,
  onErrorContainer = StudioDarkOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
  primary = StudioTeal,
  onPrimary = Color.White,
  primaryContainer = StudioTealContainer,
  onPrimaryContainer = StudioOnTealContainer,
  secondary = StudioCoral,
  onSecondary = Color.White,
  secondaryContainer = StudioCoralContainer,
  onSecondaryContainer = StudioOnCoralContainer,
  tertiary = StudioIndigo,
  onTertiary = Color.White,
  tertiaryContainer = StudioIndigoContainer,
  onTertiaryContainer = StudioOnIndigoContainer,
  background = StudioPorcelainBg,
  onBackground = StudioOnSurface,
  surface = StudioPureSurface,
  onSurface = StudioOnSurface,
  surfaceVariant = StudioSurfaceVariant,
  onSurfaceVariant = StudioOnSurfaceVariant,
  surfaceContainerLowest = StudioPureSurface,
  surfaceContainerLow = StudioSurfaceLow,
  surfaceContainer = StudioPureSurface,
  surfaceContainerHigh = StudioSurfaceHigh,
  outline = StudioOutline,
  outlineVariant = StudioOutlineVariant,
  error = StudioError,
  onError = Color.White,
  errorContainer = StudioErrorContainer,
  onErrorContainer = StudioOnErrorContainer,
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
