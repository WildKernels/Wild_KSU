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
 * UI blur effect that creates a backdrop blur behind content.
 * This applies a subtle blur effect to create a frosted glass appearance
 * while keeping text and content sharp and readable.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    val uiBlur = LocalUIBlur.current
    return if (uiBlur > 0f) {
        this.blur(radius = (uiBlur * 0.3f).dp)
    } else {
        this
    }
}

/**
 * Creates a backdrop blur wrapper that applies blur to a background layer
 * while keeping the content layer sharp and readable.
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
        // Blurred background layer
        if (uiBlur > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor.copy(alpha = 0.8f))
                    .blur(radius = (uiBlur * 0.4f).dp)
                    .zIndex(-1f)
            )
        } else {
            // Fallback non-blurred background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor)
                    .zIndex(-1f)
            )
        }
        
        // Sharp content layer
        content()
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