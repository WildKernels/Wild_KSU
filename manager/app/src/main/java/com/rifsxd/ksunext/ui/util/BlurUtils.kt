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
 * Use BackdropBlurCard instead for components with text content.
 * This ensures text remains sharp while providing backdrop blur effect.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    // Disabled to prevent text blurring
    // Use BackdropBlurCard for components that need blur with readable text
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
        // Background layer with blur
        if (uiBlur > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor.copy(alpha = 0.8f))
                    .blur(radius = (uiBlur * 25f).dp)
                    .zIndex(-1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor)
                    .zIndex(-1f)
            )
        }
        
        // Content layer (sharp, no blur)
        content()
    }
}

/**
 * A Card composable that applies backdrop blur to its background while keeping content sharp.
 * This should be used instead of applying applyUIBlur() to cards with text content.
 */
@Composable
fun BackdropBlurCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    val uiBlur = LocalUIBlur.current
    
    Box(modifier = modifier) {
        // Blurred background layer
        if (uiBlur > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .blur(radius = (uiBlur * 25f).dp)
            )
        }
        
        // Sharp content layer with subtle background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(backgroundColor.copy(alpha = if (uiBlur > 0f) 0.1f else 1f))
        ) {
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