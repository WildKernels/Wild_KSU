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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // Debug logging
    Log.d("BackgroundImage", "URI: $backgroundImageUri")
    
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
                
                // Apply simple crop transformations from saved settings
                val imageModifier = Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        val cropSettings = ImageCropUtils.loadImageCropSettings(prefs)
                        modifier.graphicsLayer(
                            scaleX = cropSettings.scale,
                            scaleY = cropSettings.scale,
                            translationX = cropSettings.offsetX,
                            translationY = cropSettings.offsetY
                        )
                    }
                
                Log.d("BackgroundImage", "Applying crop transformations")
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Content on top of background
        content()
    }
}