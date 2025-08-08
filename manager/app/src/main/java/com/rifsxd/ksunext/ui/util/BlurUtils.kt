package com.rifsxd.ksunext.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.ui.theme.LocalUIBlur

/**
 * UI blur effect - currently disabled to prevent text blurring.
 * 
 * The UI blur was causing text and content to become blurry and hard to read.
 * Instead, the frosted glass effect is achieved through the transparency system
 * in Theme.kt which makes backgrounds semi-transparent while keeping text sharp.
 * 
 * If you want blur effects, use the background blur setting which blurs the
 * background image behind the entire app.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    // Disabled to prevent text blurring - transparency provides the frosted glass effect
    return this
}

/**
 * Applies custom blur effect with specified radius.
 * Use this for specific blur effects where text readability is not a concern.
 */
fun Modifier.applyBlur(blurRadius: Float): Modifier {
    return if (blurRadius > 0f) {
        this.blur(blurRadius.dp)
    } else {
        this
    }
}