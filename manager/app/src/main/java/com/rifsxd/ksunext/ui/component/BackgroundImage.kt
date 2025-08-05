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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rifsxd.ksunext.ui.util.ImageCropUtils

@Composable
fun BackgroundImageWrapper(
    backgroundImageUri: String?,
    backgroundFitMode: String,
    backgroundTransparency: Float = 1.0f,
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
                
                // Apply transformations using enhanced ImageCropUtils
                val imageModifier = Modifier
                    .fillMaxSize()
                    .let { ImageCropUtils.getSimpleCropTransformation(prefs)(it) }
                
                Log.d("BackgroundImage", "Applying transformation for fit mode: $backgroundFitMode")
                
                val contentScale = when (backgroundFitMode) {
                    "zoom_to_fit" -> ContentScale.Crop
                    "edge_to_edge" -> ContentScale.FillBounds
                    "custom_crop" -> ContentScale.Fit
                    else -> ContentScale.FillBounds
                }
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale
                )
                
                // Add overlay with transparency control for content readability
                // Transparency slider controls how dark the overlay is (0 = no overlay, 1 = maximum overlay)
                val overlayAlpha = (1.0f - backgroundTransparency) * 0.7f // Max overlay alpha of 0.7
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = overlayAlpha)
                        )
                )
            }
        }
        
        // Content on top of background
        content()
    }
}