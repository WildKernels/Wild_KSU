package com.rifsxd.ksunext.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rifsxd.ksunext.ui.theme.LocalUIBlur

/**
 * UI blur effect - DISABLED to prevent text blurring.
 * The blur effect should be applied to background layers only.
 * Use BackdropBlurBox for components that need backdrop blur with sharp text.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    // Return unmodified to prevent text blurring
    // Blur should be applied to background layers, not content layers
    return this
}

/**
 * Creates a backdrop blur wrapper that applies blur to a background layer
 * while keeping the content layer sharp and readable.
 * This provides the frosted glass effect without blurring text.
 */
@Composable
fun BackdropBlurBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    val uiBlur = LocalUIBlur.current
    
    Box(modifier = modifier) {
        // Background layer with blur effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    if (uiBlur > 0f) {
                        backgroundColor.copy(alpha = 0.7f)
                    } else {
                        backgroundColor
                    }
                )
                .let { mod ->
                    if (uiBlur > 0f) {
                        // Apply blur to background layer only
                        mod.blur(radius = (uiBlur * 25f).dp)
                    } else {
                        mod
                    }
                }
                .zIndex(-1f)
        )
        
        // Sharp content layer on top
        Box(modifier = Modifier.zIndex(1f)) {
            content()
        }
    }
}

/**
 * Applies blur to background elements only.
 * This should be used for background layers, not for components containing text.
 */
@Composable
fun Modifier.applyBackgroundBlur(): Modifier {
    val uiBlur = LocalUIBlur.current
    return if (uiBlur > 0f) {
        // Convert 0-1.0f range to 0-25px to match background blur intensity
        this.blur(radius = (uiBlur * 25f).dp)
    } else {
        this
    }
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