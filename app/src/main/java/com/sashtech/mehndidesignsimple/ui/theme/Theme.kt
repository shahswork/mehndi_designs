package com.sashtech.mehndidesignsimple.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalGreenDark,
    onPrimary = NaturalGreenOnDark,
    primaryContainer = NaturalGreenContainerDark,
    onPrimaryContainer = NaturalGreenOnContainerDark,
    secondary = NaturalGoldDark,
    onSecondary = NaturalGoldOnDark,
    secondaryContainer = NaturalGoldContainerDark,
    onSecondaryContainer = NaturalGoldOnContainerDark,
    background = NaturalBackgroundDark,
    onBackground = NaturalOnBackgroundDark,
    surface = NaturalSurfaceDark,
    onSurface = NaturalOnSurfaceDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalOnSurfaceVariantDark,
    outline = NaturalOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalGreenPrimary,
    onPrimary = NaturalGreenOnPrimary,
    primaryContainer = NaturalGreenContainer,
    onPrimaryContainer = NaturalGreenOnContainer,
    secondary = NaturalGoldSecondary,
    onSecondary = NaturalGoldOnSecondary,
    secondaryContainer = NaturalGoldContainer,
    onSecondaryContainer = NaturalGoldOnContainer,
    tertiary = NaturalRoseTertiary,
    background = NaturalBackgroundLight,
    onBackground = NaturalOnBackgroundLight,
    surface = NaturalSurfaceLight,
    onSurface = NaturalOnSurfaceLight,
    surfaceVariant = NaturalSurfaceVariantLight,
    onSurfaceVariant = NaturalOnSurfaceVariantLight,
    outline = NaturalOutlineLight,
  )

@Composable
fun MehndiDesignTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MehndiDesignTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
