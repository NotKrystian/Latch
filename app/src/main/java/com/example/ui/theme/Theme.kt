package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Force light color scheme for Latch MVP as specified
private val LatchColorScheme = lightColorScheme(
  primary = LatchOrange,
  onPrimary = Color.White,
  secondary = LatchInk,
  onSecondary = Color.White,
  tertiary = LatchInkMuted,
  onTertiary = Color.White,
  background = LatchBg,
  onBackground = LatchInk,
  surface = LatchSurface,
  onSurface = LatchInk,
  surfaceVariant = LatchOrangeSoft,
  onSurfaceVariant = LatchOrange,
  error = LatchDanger,
  onError = Color.White
)

@Composable
fun LatchTheme(
  content: @Composable () -> Unit,
) {
  // We use our signature Latch light theme exclusively for v1
  MaterialTheme(
    colorScheme = LatchColorScheme,
    typography = Typography,
    content = content
  )
}
