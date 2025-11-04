package com.rifsxd.ksunext.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults

private val DarkColorScheme = darkColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_DARK,
    tertiary = SECONDARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_LIGHT,
    tertiary = SECONDARY_LIGHT
)

// UI blur functionality removed



fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    isCustomBackgroundEnabled: Boolean = false,
    backgroundTransparency: Float = 1.0f,
    uiTransparency: Float = 1.0f,
    content: @Composable () -> Unit
) {
    // Always apply UI transparency, regardless of background settings
    val alphaValue = 1.0f - uiTransparency
    val colorScheme = when {
        amoledMode && darkTheme && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = dynamicDarkColorScheme(context)
            val baseSurfaceVariant = dynamicScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainer = dynamicScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerLow = dynamicScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerLowest = dynamicScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerHigh = dynamicScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerHighest = dynamicScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f)
            val basePrimaryContainer = dynamicScheme.primaryContainer.blend(AMOLED_BLACK, 0.6f)
            val baseSecondaryContainer = dynamicScheme.secondaryContainer.blend(AMOLED_BLACK, 0.6f)
            val baseTertiaryContainer = dynamicScheme.tertiaryContainer.blend(AMOLED_BLACK, 0.6f)
            
            dynamicScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surfaceVariant = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainer = baseSurfaceContainer.copy(alpha = alphaValue),
                surfaceContainerLow = baseSurfaceContainerLow.copy(alpha = alphaValue),
                surfaceContainerLowest = baseSurfaceContainerLowest.copy(alpha = alphaValue),
                surfaceContainerHigh = baseSurfaceContainerHigh.copy(alpha = alphaValue),
                surfaceContainerHighest = baseSurfaceContainerHighest.copy(alpha = alphaValue),
                primaryContainer = basePrimaryContainer.copy(alpha = alphaValue),
                secondaryContainer = baseSecondaryContainer.copy(alpha = alphaValue),
                tertiaryContainer = baseTertiaryContainer.copy(alpha = alphaValue),
                outlineVariant = dynamicScheme.outlineVariant.copy(alpha = alphaValue)
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            baseScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else baseScheme.background,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else baseScheme.surface,
                surfaceVariant = baseScheme.surfaceVariant.copy(alpha = alphaValue),
                surfaceContainer = baseScheme.surfaceContainer.copy(alpha = alphaValue),
                surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = alphaValue),
                surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                surfaceContainerHighest = baseScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                primaryContainer = baseScheme.primaryContainer.copy(alpha = alphaValue),
                secondaryContainer = baseScheme.secondaryContainer.copy(alpha = alphaValue),
                tertiaryContainer = baseScheme.tertiaryContainer.copy(alpha = alphaValue),
                outlineVariant = baseScheme.outlineVariant.copy(alpha = alphaValue)
            )
        }
        amoledMode && darkTheme -> {
            val baseSurfaceVariant = DARK_GREY.blend(AMOLED_BLACK, 0.8f)
            DarkColorScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surfaceVariant = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainer = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainerLow = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainerLowest = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainerHigh = baseSurfaceVariant.copy(alpha = alphaValue),
                surfaceContainerHighest = baseSurfaceVariant.copy(alpha = alphaValue),
                outlineVariant = DarkColorScheme.outlineVariant.copy(alpha = alphaValue)
            )
        }
        darkTheme -> {
            DarkColorScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else DarkColorScheme.background,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else DarkColorScheme.surface,
                surfaceVariant = DarkColorScheme.surfaceVariant.copy(alpha = alphaValue),
                surfaceContainer = DarkColorScheme.surfaceContainer.copy(alpha = alphaValue),
                surfaceContainerLow = DarkColorScheme.surfaceContainerLow.copy(alpha = alphaValue),
                surfaceContainerLowest = DarkColorScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                surfaceContainerHigh = DarkColorScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                surfaceContainerHighest = DarkColorScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                primaryContainer = DarkColorScheme.primaryContainer.copy(alpha = alphaValue),
                secondaryContainer = DarkColorScheme.secondaryContainer.copy(alpha = alphaValue),
                tertiaryContainer = DarkColorScheme.tertiaryContainer.copy(alpha = alphaValue),
                outlineVariant = DarkColorScheme.outlineVariant.copy(alpha = alphaValue)
            )
        }
        else -> {
            LightColorScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else LightColorScheme.background,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else LightColorScheme.surface,
                surfaceVariant = LightColorScheme.surfaceVariant.copy(alpha = alphaValue),
                surfaceContainer = LightColorScheme.surfaceContainer.copy(alpha = alphaValue),
                surfaceContainerLow = LightColorScheme.surfaceContainerLow.copy(alpha = alphaValue),
                surfaceContainerLowest = LightColorScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                surfaceContainerHigh = LightColorScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                surfaceContainerHighest = LightColorScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                primaryContainer = LightColorScheme.primaryContainer.copy(alpha = alphaValue),
                secondaryContainer = LightColorScheme.secondaryContainer.copy(alpha = alphaValue),
                tertiaryContainer = LightColorScheme.tertiaryContainer.copy(alpha = alphaValue),
                outlineVariant = LightColorScheme.outlineVariant.copy(alpha = alphaValue)
            )
        }
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}

/**
 * Get card elevation with no shadow/border
 * Always returns 0.dp elevation for a clean, flat appearance
 */
@Composable
fun getCardElevation(): androidx.compose.material3.CardElevation {
    return CardDefaults.elevatedCardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        draggedElevation = 0.dp,
        disabledElevation = 0.dp
    )
}
