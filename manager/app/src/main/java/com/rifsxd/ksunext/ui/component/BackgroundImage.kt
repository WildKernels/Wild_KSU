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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rifsxd.ksunext.ui.util.BackgroundCustomization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.compose.AsyncImagePainter

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
    
    // Debug logging - only log when values change
    remember(backgroundImageUri, backgroundFitMode, backgroundTransparency, backgroundBlur) {
        Log.d("BackgroundImage", "URI: $backgroundImageUri, FitMode: $backgroundFitMode, Transparency: $backgroundTransparency, Blur: $backgroundBlur")
    }
    
    // State for blurred image - reset when backgroundImageUri or backgroundBlur changes
    var blurredPainter by remember(backgroundImageUri, backgroundBlur) { mutableStateOf<BitmapPainter?>(null) }
    var isProcessingBlur by remember(backgroundImageUri, backgroundBlur) { mutableStateOf(false) }
    var lastProcessedBlur by remember(backgroundImageUri) { mutableStateOf(-1f) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Display background image if available
        backgroundImageUri?.let { uriString ->
            if (uriString.isNotEmpty()) {
                // Log only when URI changes
                remember(uriString) {
                    Log.d("BackgroundImage", "Loading image from URI: $uriString")
                }
                
                // Validate and parse URI - memoized to prevent continuous parsing
                val validatedUri = remember(uriString) {
                    try {
                        val uri = Uri.parse(uriString)
                        Log.d("BackgroundImage", "Parsed URI scheme: ${uri.scheme}, authority: ${uri.authority}")
                        uri
                    } catch (e: Exception) {
                        Log.e("BackgroundImage", "Invalid URI: $uriString", e)
                        null
                    }
                }
                
                if (validatedUri == null) return@let
                
                val originalPainter = rememberAsyncImagePainter(
                    model = remember(uriString) {
                        ImageRequest.Builder(context)
                            .data(validatedUri)
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
                    }
                )
                
                // Clear blur immediately when backgroundBlur becomes 0
                LaunchedEffect(backgroundBlur) {
                    if (backgroundBlur <= 0f && blurredPainter != null) {
                        Log.d("BackgroundImage", "Clearing blur - backgroundBlur: $backgroundBlur")
                        blurredPainter = null
                        lastProcessedBlur = -1f
                    }
                }
                
                // Apply blur effect only when needed and blur value has actually changed
                LaunchedEffect(backgroundBlur, uriString) {
                    if (backgroundBlur > 0f && 
                        originalPainter.state is AsyncImagePainter.State.Success && 
                        lastProcessedBlur != backgroundBlur &&
                        !isProcessingBlur) {
                        Log.d("BackgroundImage", "LaunchedEffect triggered - backgroundBlur: $backgroundBlur, painter state: ${originalPainter.state}")
                        Log.d("BackgroundImage", "Starting blur processing with radius: $backgroundBlur")
                        isProcessingBlur = true
                        try {
                            val drawable = (originalPainter.state as AsyncImagePainter.State.Success).result.drawable
                            if (drawable is BitmapDrawable) {
                                val bitmap = drawable.bitmap
                                Log.d("BackgroundImage", "Bitmap size: ${bitmap.width}x${bitmap.height}")
                                Log.d("BackgroundImage", "Applying simple blur with radius: $backgroundBlur")
                                val blurredBitmap = withContext(Dispatchers.Default) {
                                    BackgroundCustomization.applyBlur(bitmap, backgroundBlur)
                                }
                                blurredPainter = BitmapPainter(blurredBitmap.asImageBitmap())
                                lastProcessedBlur = backgroundBlur
                                Log.d("BackgroundImage", "Blur processing completed successfully")
                            } else {
                                Log.w("BackgroundImage", "Drawable is not BitmapDrawable: ${drawable::class.java.simpleName}")
                            }
                        } catch (e: Exception) {
                            Log.e("BackgroundImage", "Failed to apply blur: ${e.message}", e)
                            blurredPainter = null
                        } finally {
                            isProcessingBlur = false
                        }
                    }
                }
                
                // Use blurred painter if available, otherwise use original - memoized to prevent continuous logging
                val painter = remember(backgroundBlur, blurredPainter, originalPainter) {
                    if (backgroundBlur > 0f && blurredPainter != null) {
                        Log.d("BackgroundImage", "Using blurred painter")
                        blurredPainter!!
                    } else {
                        Log.d("BackgroundImage", "Using original painter - backgroundBlur: $backgroundBlur, blurredPainter: $blurredPainter")
                        originalPainter
                    }
                }
                
                // Load transform settings from SharedPreferences - remember to prevent continuous reads
                val scale = remember { prefs.getFloat("background_scale_x", 1f) }
                val offsetX = remember { prefs.getFloat("background_pos_x", 0f) }
                val offsetY = remember { prefs.getFloat("background_pos_y", 0f) }
                val rotation = remember { prefs.getFloat("background_rotation", 0f) }
                val flipHorizontal = remember { prefs.getBoolean("background_flip_horizontal", false) }
                val flipVertical = remember { prefs.getBoolean("background_flip_vertical", false) }
                
                // Apply transformations (blur is now handled by custom painter)
                val imageModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale * (if (flipHorizontal) -1f else 1f)
                        scaleY = scale * (if (flipVertical) -1f else 1f)
                        translationX = offsetX
                        translationY = offsetY
                        rotationZ = rotation
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