package com.rifsxd.ksunext.ui.component

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

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
                        .crossfade(false)
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
                
                // Load transform settings from SharedPreferences
                val scale = prefs.getFloat("background_scale_x", 1f)
                val offsetX = prefs.getFloat("background_pos_x", 0f)
                val offsetY = prefs.getFloat("background_pos_y", 0f)
                val rotation = prefs.getFloat("background_rotation", 0f)
                val flipHorizontal = prefs.getBoolean("background_flip_horizontal", false)
                val flipVertical = prefs.getBoolean("background_flip_vertical", false)
                
                Log.d("BackgroundImage", "Transform settings: scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation, flipH=$flipHorizontal, flipV=$flipVertical")
                
                // Apply transformations and blur
                val imageModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale * (if (flipHorizontal) -1f else 1f)
                        scaleY = scale * (if (flipVertical) -1f else 1f)
                        translationX = offsetX
                        translationY = offsetY
                        rotationZ = rotation
                    }
                    .let { modifier ->
                        if (backgroundBlur > 0f) {
                            modifier.blur(backgroundBlur.dp)
                        } else {
                            modifier
                        }
                    }
                
                // Use ContentScale based on fit mode
                val contentScale = when (backgroundFitMode) {
                    "fill" -> ContentScale.FillBounds
                    "crop" -> ContentScale.Crop
                    "center" -> ContentScale.None
                    "fit" -> ContentScale.Fit
                    else -> ContentScale.Fit
                }
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale,
                    alignment = androidx.compose.ui.Alignment.Center
                )
            }
        }
        
        // Overlay with darkness control for content readability
        val overlayAlpha = backgroundTransparency
        Log.d("BackgroundImage", "Overlay alpha: $overlayAlpha")
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