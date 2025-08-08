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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
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
                
                var imageLoaded by remember { mutableStateOf(false) }
                var imageError by remember { mutableStateOf<String?>(null) }
                
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(uriString))
                        .crossfade(true)
                        .listener(
                            onStart = { 
                                Log.d("BackgroundImage", "Started loading image")
                                imageLoaded = false
                                imageError = null
                            },
                            onSuccess = { _, _ -> 
                                Log.d("BackgroundImage", "Successfully loaded image")
                                imageLoaded = true
                                imageError = null
                            },
                            onError = { _, result -> 
                                Log.e("BackgroundImage", "Failed to load image: ${result.throwable}")
                                imageLoaded = false
                                imageError = result.throwable.message
                            }
                        )
                        .build()
                )
                
                Log.d("BackgroundImage", "Image loaded: $imageLoaded, Error: $imageError")
                
                // Use custom_crop as default fit mode for advanced editor
                val effectiveFitMode = if (backgroundFitMode.isEmpty()) "custom_crop" else backgroundFitMode
                
                // Apply transformations using enhanced ImageCropUtils
                val imageModifier = Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        val transformation = ImageCropUtils.getImageTransformation(prefs, effectiveFitMode)
                        modifier.transformation()
                    }
                    .let { modifier ->
                        // Apply blur if specified
                        if (backgroundBlur > 0f) {
                            modifier.blur(backgroundBlur.dp)
                        } else {
                            modifier
                        }
                    }
                
                Log.d("BackgroundImage", "Applying transformation for fit mode: $effectiveFitMode")
                
                // Load saved crop settings for debugging
                val cropSettings = ImageCropUtils.loadImageCropSettings(prefs)
                Log.d("BackgroundImage", "Loaded crop settings: scale=${cropSettings.scale}, offsetX=${cropSettings.offsetX}, offsetY=${cropSettings.offsetY}, rotation=${cropSettings.rotation}")
                
                // Always use ContentScale.Fit to preserve aspect ratio for custom crop
                val contentScale = ContentScale.Fit
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale
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