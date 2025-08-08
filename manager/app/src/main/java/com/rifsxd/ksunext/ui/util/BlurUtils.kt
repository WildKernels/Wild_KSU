package com.rifsxd.ksunext.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.ui.theme.LocalUIBlur

/**
 * UI blur effect that works on the same layer as UI transparency.
 * Currently disabled to prevent whole-app blurring. The blur effect
 * is now handled through the transparency system in the theme.
 * 
 * This function is kept for compatibility but returns the modifier unchanged.
 * The UI blur effect is achieved through the combination of transparency
 * and background effects in the theme system.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    // Disabled to prevent component-level blurring that affects the entire UI
    // UI blur now works through the transparency system for better integration
    return this
}

/**
 * Applies custom blur effect with specified radius
 */
fun Modifier.applyBlur(blurRadius: Float): Modifier {
    return if (blurRadius > 0f) {
        this.blur(blurRadius.dp)
    } else {
        this
    }
}