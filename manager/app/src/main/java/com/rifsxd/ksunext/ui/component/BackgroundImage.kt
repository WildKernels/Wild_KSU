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
                        transformation(modifier)
                    }
                    .let { modifier ->
                        // Apply stored color adjustments from PhotoEditor
                        val brightness = prefs.getFloat("edit_brightness", 0f)
                        val contrast = prefs.getFloat("edit_contrast", 0f)
                        val saturation = prefs.getFloat("edit_saturation", 0f)
                        val hue = prefs.getFloat("edit_hue", 0f)
                        val flipHorizontal = prefs.getBoolean("edit_flip_horizontal", false)
                        val flipVertical = prefs.getBoolean("edit_flip_vertical", false)
                        
                        if (brightness != 0f || contrast != 0f || saturation != 0f || hue != 0f || flipHorizontal || flipVertical) {
                            // Create color matrix for image adjustments
                            val colorMatrix = remember(brightness, contrast, saturation, hue) {
                                androidx.compose.ui.graphics.ColorMatrix().apply {
                                    // Apply saturation first (convert from -100/100 range to 0-2 range)
                                    val saturationValue = (saturation + 100f) / 100f
                                    setToSaturation(saturationValue)
                                    
                                    // Apply hue rotation using manual matrix calculation
                                    if (hue != 0f) {
                                        val hueRadians = hue * kotlin.math.PI / 180f
                                        val cosHue = kotlin.math.cos(hueRadians).toFloat()
                                        val sinHue = kotlin.math.sin(hueRadians).toFloat()
                                        
                                        // Create hue rotation matrix manually
                                        val hueMatrix = floatArrayOf(
                                            0.213f + cosHue * 0.787f - sinHue * 0.213f, 0.715f - cosHue * 0.715f - sinHue * 0.715f, 0.072f - cosHue * 0.072f + sinHue * 0.928f, 0f, 0f,
                                            0.213f - cosHue * 0.213f + sinHue * 0.143f, 0.715f + cosHue * 0.285f + sinHue * 0.140f, 0.072f - cosHue * 0.072f - sinHue * 0.283f, 0f, 0f,
                                            0.213f - cosHue * 0.213f - sinHue * 0.787f, 0.715f - cosHue * 0.715f + sinHue * 0.715f, 0.072f + cosHue * 0.928f + sinHue * 0.072f, 0f, 0f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                        postConcat(androidx.compose.ui.graphics.ColorMatrix(hueMatrix))
                                    }
                                    
                                    // Apply brightness and contrast
                                    val brightnessMatrix = androidx.compose.ui.graphics.ColorMatrix().apply {
                                        val brightnessValue = brightness / 100f
                                        val contrastValue = (contrast + 100f) / 100f
                                        val contrastTranslate = (1f - contrastValue) * 0.5f
                                        
                                        setToScale(
                                            contrastValue, contrastValue, contrastValue, 1f
                                        )
                                        
                                        val values = colorMatrix
                                        values[4] = brightnessValue + contrastTranslate // R offset
                                        values[9] = brightnessValue + contrastTranslate // G offset
                                        values[14] = brightnessValue + contrastTranslate // B offset
                                    }
                                    postConcat(brightnessMatrix)
                                }
                            }
                            
                            modifier
                                .graphicsLayer {
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(colorMatrix)
                                    scaleX = if (flipHorizontal) -1f else 1f
                                    scaleY = if (flipVertical) -1f else 1f
                                }
                        } else {
                            modifier
                        }
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