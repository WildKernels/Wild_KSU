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
 * UI blur effect with very subtle intensity to maintain text readability.
 * Uses 0-100% scale where 100% = 25px blur (same as background blur maximum).
 * Applied with minimal intensity (5%) to provide frosted glass effect while keeping text readable.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    val uiBlur = LocalUIBlur.current
    return if (uiBlur > 0f) {
        // Apply moderate blur (30% intensity) for visible frosted glass effect
        // This provides a good balance between visual effect and text readability
        this.blur(radius = (uiBlur * 25f * 0.3f).dp)
    } else {
        this
    }
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
 * A layered card that applies blur effect only to the background while keeping content completely sharp.
 * This creates a frosted glass effect without affecting text readability.
 */
@Composable
fun LayeredBlurCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: androidx.compose.material3.CardElevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(),
    content: @Composable () -> Unit
) {
    val uiBlur = LocalUIBlur.current
    
    Box(modifier = modifier) {
        // Blurred background layer - completely separate from content
        if (uiBlur > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(backgroundColor)
                    .blur(radius = (uiBlur * 25f).dp) // Full blur intensity
            )
        }
        
        // Transparent content layer - NO blur applied here
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.matchParentSize(),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = if (uiBlur > 0f) Color.Transparent else backgroundColor
            ),
            shape = shape,
            elevation = elevation
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