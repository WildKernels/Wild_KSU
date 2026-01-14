package com.rifsxd.ksunext.ui.theme

import android.os.Build
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.remember
import com.google.android.material.color.utilities.CorePalette
import com.google.android.material.color.utilities.Scheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette


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
    appTheme: AppTheme = AppTheme.AUTO,
    customColor: Color? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val (colorScheme, darkTheme) = when (appTheme) {
        AppTheme.AUTO -> {
            val scheme = if (dynamicColor) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) DarkColorScheme else LightColorScheme
            }
            scheme to systemDark
        }
        AppTheme.DARK_DYNAMIC -> {
            val scheme = if (dynamicColor) dynamicDarkColorScheme(context) else DarkColorScheme
            scheme to true
        }
        AppTheme.LIGHT_DYNAMIC -> {
            val scheme = if (dynamicColor) dynamicLightColorScheme(context) else LightColorScheme
            scheme to false
        }
        AppTheme.LIGHT -> LightColorScheme.copy(onBackground = Color.Black, onSurface = Color.Black) to false
        AppTheme.DARK -> DarkColorScheme.copy(onBackground = Color.White, onSurface = Color.White) to true
        AppTheme.AMOLED -> {
            val baseScheme = if (dynamicColor) dynamicDarkColorScheme(context) else DarkColorScheme
            val amoledScheme = baseScheme.copy(
                background = AMOLED_BLACK,
                surface = AMOLED_BLACK,
                onBackground = Color.White,
                onSurface = Color.White,
                surfaceVariant = baseScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f),
                surfaceContainer = baseScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLow = baseScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerLowest = baseScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHigh = baseScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f),
                surfaceContainerHighest = baseScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f),
                primaryContainer = baseScheme.primaryContainer.blend(AMOLED_BLACK, 0.6f),
                secondaryContainer = baseScheme.secondaryContainer.blend(AMOLED_BLACK, 0.6f),
                onTertiaryContainer = baseScheme.onTertiaryContainer.blend(AMOLED_BLACK, 0.6f)
            )
            amoledScheme to true
        }
        AppTheme.CUSTOM -> {
            val colorToUse = customColor ?: Color(prefs.getInt("theme_custom_color", PRIMARY.toArgb()))
            val customBaseMode = prefs.getString("theme_custom_base_mode", "system")
            val customTextColorArgb = prefs.getInt("theme_custom_text_color", 0)

            val useDark = when (customBaseMode) {
                "light" -> false
                "dark", "amoled" -> true
                else -> systemDark
            }

            val argb = colorToUse.toArgb()

            val scheme = if (useDark) Scheme.dark(argb) else Scheme.light(argb)
            val corePalette = CorePalette.of(argb)
            val hct = Hct.fromInt(colorToUse.toArgb().toInt())
            val neutralPalette = TonalPalette.fromHueAndChroma(hct.hue, 4.0)

            var m3Scheme = ColorScheme(
                primary = Color(scheme.primary),
                onPrimary = Color(scheme.onPrimary),
                primaryContainer = Color(scheme.primaryContainer),
                onPrimaryContainer = Color(scheme.onPrimaryContainer),
                inversePrimary = Color(scheme.inversePrimary),
                secondary = Color(scheme.secondary),
                onSecondary = Color(scheme.onSecondary),
                secondaryContainer = Color(scheme.secondaryContainer),
                onSecondaryContainer = Color(scheme.onSecondaryContainer),
                tertiary = Color(scheme.tertiary),
                onTertiary = Color(scheme.onTertiary),
                tertiaryContainer = Color(scheme.tertiaryContainer),
                onTertiaryContainer = Color(scheme.onTertiaryContainer),
                background = Color(scheme.background),
                onBackground = Color(scheme.onBackground),
                surface = Color(scheme.surface),
                onSurface = Color(scheme.onSurface),
                surfaceVariant = Color(scheme.surfaceVariant),
                onSurfaceVariant = Color(scheme.onSurfaceVariant),
                surfaceTint = Color(scheme.primary),
                inverseSurface = Color(scheme.inverseSurface),
                inverseOnSurface = Color(scheme.inverseOnSurface),
                error = Color(scheme.error),
                onError = Color(scheme.onError),
                errorContainer = Color(scheme.errorContainer),
                onErrorContainer = Color(scheme.onErrorContainer),
                outline = Color(scheme.outline),
                outlineVariant = Color(scheme.outlineVariant),
                scrim = Color(scheme.scrim),

                // Surface Container Roles (derived from Neutral Palette)
                surfaceBright = Color(neutralPalette.tone(if (useDark) 24 else 98)),
                surfaceDim = Color(neutralPalette.tone(if (useDark) 6 else 87)),
                surfaceContainer = Color(neutralPalette.tone(if (useDark) 12 else 94)),
                surfaceContainerHigh = Color(neutralPalette.tone(if (useDark) 17 else 92)),
                surfaceContainerHighest = Color(neutralPalette.tone(if (useDark) 22 else 90)),
                surfaceContainerLow = Color(neutralPalette.tone(if (useDark) 10 else 96)),
                surfaceContainerLowest = Color(neutralPalette.tone(if (useDark) 4 else 100))
            )

            if (customBaseMode == "amoled") {
                m3Scheme = m3Scheme.copy(
                    background = AMOLED_BLACK,
                    surface = AMOLED_BLACK,
                    surfaceVariant = m3Scheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainer = m3Scheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerLow = m3Scheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerLowest = m3Scheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerHigh = m3Scheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerHighest = m3Scheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f),
                    primaryContainer = m3Scheme.primaryContainer.blend(AMOLED_BLACK, 0.6f),
                    secondaryContainer = m3Scheme.secondaryContainer.blend(AMOLED_BLACK, 0.6f),
                    onTertiaryContainer = m3Scheme.onTertiaryContainer.blend(AMOLED_BLACK, 0.6f)
                )
            }

            if (customTextColorArgb != 0) {
                val textColor = Color(customTextColorArgb)
                m3Scheme = m3Scheme.copy(
                    onBackground = textColor,
                    onSurface = textColor
                )
            }

            m3Scheme to useDark
        }
    }

    SystemBarStyle(
        darkMode = darkTheme,
        navigationBarColor = colorScheme.surfaceContainer
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
    navigationBarColor: Color? = null,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val density = LocalDensity.current
    val insets = WindowInsets.systemGestures
    
    // Check for 3-button navigation by inspecting system gesture insets.
    // Gesture navigation usually has non-zero left/right system gesture insets.
    // 3-button navigation has zero left/right system gesture insets.
    val isThreeButtonNav = remember(insets, density) {
        val left = insets.getLeft(density, LayoutDirection.Ltr)
        val right = insets.getRight(density, LayoutDirection.Ltr)
        left == 0 && right == 0
    }

    val actualNavigationBarScrim = if (isThreeButtonNav && navigationBarColor != null) {
        navigationBarColor
    } else if (isThreeButtonNav) {
        if (darkMode) {
            Color.Black.copy(alpha = 0.5f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }
    } else {
        navigationBarScrim
    }

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    actualNavigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    actualNavigationBarScrim.toArgb(),
                    actualNavigationBarScrim.toArgb(),
                )
            }
        )
    }
}