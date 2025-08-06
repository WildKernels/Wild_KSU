package com.rifsxd.ksunext.ui.theme

import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider

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

// CompositionLocal for UI transparency
val LocalUITransparency = compositionLocalOf { 0.0f }

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
    // Convert transparency percentage to alpha value (invert so 0% = solid, 100% = transparent)
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
                surfaceVariant = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainer = if (isCustomBackgroundEnabled) baseSurfaceContainer.copy(alpha = alphaValue) else baseSurfaceContainer,
                surfaceContainerLow = if (isCustomBackgroundEnabled) baseSurfaceContainerLow.copy(alpha = alphaValue) else baseSurfaceContainerLow,
                surfaceContainerLowest = if (isCustomBackgroundEnabled) baseSurfaceContainerLowest.copy(alpha = alphaValue) else baseSurfaceContainerLowest,
                surfaceContainerHigh = if (isCustomBackgroundEnabled) baseSurfaceContainerHigh.copy(alpha = alphaValue) else baseSurfaceContainerHigh,
                surfaceContainerHighest = if (isCustomBackgroundEnabled) baseSurfaceContainerHighest.copy(alpha = alphaValue) else baseSurfaceContainerHighest,
                primaryContainer = if (isCustomBackgroundEnabled) basePrimaryContainer.copy(alpha = alphaValue) else basePrimaryContainer,
                secondaryContainer = if (isCustomBackgroundEnabled) baseSecondaryContainer.copy(alpha = alphaValue) else baseSecondaryContainer,
                tertiaryContainer = if (isCustomBackgroundEnabled) baseTertiaryContainer.copy(alpha = alphaValue) else baseTertiaryContainer
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isCustomBackgroundEnabled) {
                baseScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = baseScheme.surfaceVariant.copy(alpha = alphaValue),
                    surfaceContainer = baseScheme.surfaceContainer.copy(alpha = alphaValue),
                    surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = alphaValue),
                    surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                    surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                    surfaceContainerHighest = baseScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                    primaryContainer = baseScheme.primaryContainer.copy(alpha = alphaValue),
                    secondaryContainer = baseScheme.secondaryContainer.copy(alpha = alphaValue),
                    tertiaryContainer = baseScheme.tertiaryContainer.copy(alpha = alphaValue)
                )
            } else {
                baseScheme
            }
        }
        amoledMode && darkTheme -> {
            val baseSurfaceVariant = DARK_GREY.blend(AMOLED_BLACK, 0.8f)
            DarkColorScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surfaceVariant = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainer = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainerLow = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainerLowest = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainerHigh = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
                surfaceContainerHighest = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = alphaValue) else baseSurfaceVariant,
            )
        }
        darkTheme -> {
            if (isCustomBackgroundEnabled) {
                DarkColorScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = DarkColorScheme.surfaceVariant.copy(alpha = alphaValue),
                    surfaceContainer = DarkColorScheme.surfaceContainer.copy(alpha = alphaValue),
                    surfaceContainerLow = DarkColorScheme.surfaceContainerLow.copy(alpha = alphaValue),
                    surfaceContainerLowest = DarkColorScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                    surfaceContainerHigh = DarkColorScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                    surfaceContainerHighest = DarkColorScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                    primaryContainer = DarkColorScheme.primaryContainer.copy(alpha = alphaValue),
                    secondaryContainer = DarkColorScheme.secondaryContainer.copy(alpha = alphaValue),
                    tertiaryContainer = DarkColorScheme.tertiaryContainer.copy(alpha = alphaValue)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> {
            if (isCustomBackgroundEnabled) {
                LightColorScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = LightColorScheme.surfaceVariant.copy(alpha = alphaValue),
                    surfaceContainer = LightColorScheme.surfaceContainer.copy(alpha = alphaValue),
                    surfaceContainerLow = LightColorScheme.surfaceContainerLow.copy(alpha = alphaValue),
                    surfaceContainerLowest = LightColorScheme.surfaceContainerLowest.copy(alpha = alphaValue),
                    surfaceContainerHigh = LightColorScheme.surfaceContainerHigh.copy(alpha = alphaValue),
                    surfaceContainerHighest = LightColorScheme.surfaceContainerHighest.copy(alpha = alphaValue),
                    primaryContainer = LightColorScheme.primaryContainer.copy(alpha = alphaValue),
                    secondaryContainer = LightColorScheme.secondaryContainer.copy(alpha = alphaValue),
                    tertiaryContainer = LightColorScheme.tertiaryContainer.copy(alpha = alphaValue)
                )
            } else {
                LightColorScheme
            }
        }
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalUITransparency provides uiTransparency,
            content = content
        )
    }
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
 * Get dynamic card elevation based on transparency level
 * When transparency is high (> 0.7), elevation becomes 0 to remove borders
 */
@Composable
fun getTransparencyAwareCardElevation(): androidx.compose.material3.CardElevation {
    val uiTransparency = LocalUITransparency.current
    val elevation = if (uiTransparency > 0.7f) 0.dp else 6.dp
    return CardDefaults.elevatedCardElevation(
        defaultElevation = elevation,
        pressedElevation = elevation,
        focusedElevation = elevation,
        hoveredElevation = elevation,
        draggedElevation = elevation,
        disabledElevation = 0.dp
    )
}