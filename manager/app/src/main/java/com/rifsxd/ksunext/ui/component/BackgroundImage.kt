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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.preference.PreferenceManager
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
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    // Debug logging
    Log.d("BackgroundImage", "URI: $backgroundImageUri, FitMode: $backgroundFitMode")
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Display background image if available
        backgroundImageUri?.let { uriString ->
            if (uriString.isNotEmpty()) {
                Log.d("BackgroundImage", "Loading image from URI: $uriString")
                
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(uriString))
                        .crossfade(true)
                        .build()
                )
                
                // Load crop settings
                val cropSettings = remember(uriString) {
                    ImageCropUtils.loadImageCropSettings(prefs)
                }
                
                val contentScale = when (backgroundFitMode) {
                    "zoom_to_fit" -> ContentScale.Crop
                    "edge_to_edge" -> ContentScale.FillBounds
                    "custom_crop" -> ContentScale.Fit
                    else -> ContentScale.FillBounds
                }
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (backgroundFitMode == "custom_crop") cropSettings.scale else 1f,
                            scaleY = if (backgroundFitMode == "custom_crop") cropSettings.scale else 1f,
                            translationX = if (backgroundFitMode == "custom_crop") cropSettings.offsetX else 0f,
                            translationY = if (backgroundFitMode == "custom_crop") cropSettings.offsetY else 0f
                        ),
                    contentScale = contentScale
                )
                
                // Add a very light semi-transparent overlay to ensure content readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.1f)
                        )
                )
            }
        }
        
        // Content on top of background
        content()
    }
}