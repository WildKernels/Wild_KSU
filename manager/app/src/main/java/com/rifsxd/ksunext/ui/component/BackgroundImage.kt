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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rifsxd.ksunext.ui.util.BackgroundEditorUtils

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
                
                // Apply transformations using enhanced BackgroundEditorUtils
                val imageModifier = Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        val transformation = BackgroundEditorUtils.getImageTransformation(prefs, effectiveFitMode)
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
                
                // Load saved transform settings for debugging
                val transformSettings = BackgroundEditorUtils.loadImageTransformSettings(prefs)
                Log.d("BackgroundImage", "Loaded transform settings: scale=${transformSettings.scale}, offsetX=${transformSettings.offsetX}, offsetY=${transformSettings.offsetY}, rotation=${transformSettings.rotation}")
                
                // Load color adjustment settings from SharedPreferences
                val brightness = prefs.getFloat("image_brightness", 0f)
                val contrast = prefs.getFloat("image_contrast", 1f)
                val saturation = prefs.getFloat("image_saturation", 1f)
                val hue = prefs.getFloat("image_hue", 0f)
                
                // Create color matrix for adjustments (matching PhotoEditor logic)
                val colorMatrix = remember(brightness, contrast, saturation, hue) {
                    ColorMatrix().apply {
                        // Apply brightness (-200 to 200 range)
                        val brightnessValue = brightness / 255f
                        set(0, 4, brightnessValue) // Red offset
                        set(1, 4, brightnessValue) // Green offset
                        set(2, 4, brightnessValue) // Blue offset
                        
                        // Apply contrast (0 to 4 range)
                        val contrastValue = contrast
                        set(0, 0, contrastValue) // Red scale
                        set(1, 1, contrastValue) // Green scale
                        set(2, 2, contrastValue) // Blue scale
                        
                        // Apply saturation (0 to 3 range)
                        val saturationMatrix = ColorMatrix()
                        saturationMatrix.setToSaturation(saturation)
                        this.timesAssign(saturationMatrix)
                        
                        // Apply hue rotation (-360 to 360 range)
                        if (hue != 0f) {
                            val hueMatrix = ColorMatrix()
                            hueMatrix.setToRotateRed(hue)
                            this.timesAssign(hueMatrix)
                        }
                    }
                }
                
                Log.d("BackgroundImage", "Loaded adjustment settings: brightness=$brightness, contrast=$contrast, saturation=$saturation, hue=$hue")
                
                // Use ContentScale.Fit for fit mode (used for pre-transformed images), ContentScale.Crop for custom_crop
                val contentScale = when (effectiveFitMode) {
                    "fill" -> ContentScale.FillBounds
                    "crop" -> ContentScale.Crop
                    "custom_crop" -> ContentScale.Crop
                    "center" -> ContentScale.None
                    else -> ContentScale.Fit
                }
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale,
                    alignment = androidx.compose.ui.Alignment.Center,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
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