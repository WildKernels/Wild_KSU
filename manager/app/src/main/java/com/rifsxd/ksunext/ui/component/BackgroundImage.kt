package com.rifsxd.ksunext.ui.component

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rifsxd.ksunext.ui.util.ImageCropUtils

@Composable
fun BackgroundImageWrapper(
    backgroundImageUri: String?,
    backgroundFitMode: String,
    backgroundTransparency: Float = 1.0f,
    backgroundBlur: Float = 0.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // Debug logging
    Log.d("BackgroundImage", "URI: $backgroundImageUri, FitMode: $backgroundFitMode")
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Display background image if available
        backgroundImageUri?.let { uriString ->
            if (uriString.isNotEmpty()) {
                Log.d("BackgroundImage", "Loading image from URI: $uriString")
                
                // Validate URI
                try {
                    val uri = Uri.parse(uriString)
                    Log.d("BackgroundImage", "Parsed URI scheme: ${uri.scheme}, authority: ${uri.authority}")
                } catch (e: Exception) {
                    Log.e("BackgroundImage", "Invalid URI: $uriString", e)
                    return@let
                }
                
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(uriString))
                        .crossfade(false) // Disable crossfade to prevent flashing
                        .listener(
                            onStart = { 
                                Log.d("BackgroundImage", "Started loading image")
                            },
                            onSuccess = { _, _ -> 
                                Log.d("BackgroundImage", "Successfully loaded image")
                            },
                            onError = { _, result -> 
                                Log.e("BackgroundImage", "Failed to load image: ${result.throwable}")
                            }
                        )
                        .build()
                )
                
                // Only apply transformations when explicitly set, default to no transformation
                val effectiveFitMode = if (backgroundFitMode.isEmpty()) "fit" else backgroundFitMode
                
                // Apply transformations using enhanced ImageCropUtils
                val imageModifier = Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        val transformation = ImageCropUtils.getImageTransformation(prefs, effectiveFitMode)
                        transformation(modifier)
                    }
                    .let { modifier ->
                        // Apply blur if specified
                        // Convert 0-1.0f range to 0-25px to match UI blur intensity
                        if (backgroundBlur > 0f) {
                            modifier.blur((backgroundBlur * 25f).dp)
                        } else {
                            modifier
                        }
                    }
                
                Log.d("BackgroundImage", "Applying transformation for fit mode: $effectiveFitMode")
                
                // Load saved crop settings for debugging
                val cropSettings = ImageCropUtils.loadImageCropSettings(prefs)
                Log.d("BackgroundImage", "Loaded crop settings: scale=${cropSettings.scale}, offsetX=${cropSettings.offsetX}, offsetY=${cropSettings.offsetY}, rotation=${cropSettings.rotation}")
                
                // Use ContentScale.Fit to match editor behavior and preserve aspect ratio
                val contentScale = ContentScale.Fit
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale,
                    alignment = androidx.compose.ui.Alignment.Center
                )
            }
        }
        
        // Always add overlay with darkness control for content readability
        // This works both with and without background images
        // Darkness slider: 1.0f = 100% (full black overlay), 0.0f = 0% (no overlay)
        val overlayAlpha = backgroundTransparency // Direct mapping: 1.0f = full black, 0.0f = transparent
        Log.d("BackgroundImage", "Overlay alpha: $overlayAlpha (from transparency: $backgroundTransparency)")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = overlayAlpha)
                )
        )
        
        // Content on top of background
        content()
    }
}