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
import com.rifsxd.ksunext.ui.util.BlurUtils
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
                
                // State for blurred image
                var blurredPainter by remember { mutableStateOf<BitmapPainter?>(null) }
                var isProcessingBlur by remember { mutableStateOf(false) }
                
                val originalPainter = rememberAsyncImagePainter(
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
                
                // Apply blur effect when needed
                LaunchedEffect(backgroundBlur, originalPainter.state) {
                    if (backgroundBlur > 0f && originalPainter.state is AsyncImagePainter.State.Success) {
                        isProcessingBlur = true
                        try {
                            val drawable = (originalPainter.state as AsyncImagePainter.State.Success).result.drawable
                            if (drawable is BitmapDrawable) {
                                val bitmap = drawable.bitmap
                                val blurredBitmap = withContext(Dispatchers.Default) {
                                    when (BlurUtils.getBestBlurMethod(backgroundBlur)) {
                                        BlurUtils.BlurMethod.BOX_BLUR -> BlurUtils.applyBoxBlur(bitmap, backgroundBlur)
                                        BlurUtils.BlurMethod.GAUSSIAN_BLUR -> BlurUtils.applyGaussianBlur(bitmap, backgroundBlur)
                                        BlurUtils.BlurMethod.RENDER_EFFECT -> bitmap // Fallback to original for RenderEffect
                                    }
                                }
                                blurredPainter = BitmapPainter(blurredBitmap.asImageBitmap())
                            }
                        } catch (e: Exception) {
                            Log.e("BackgroundImage", "Failed to apply blur: ${e.message}", e)
                            blurredPainter = null
                        } finally {
                            isProcessingBlur = false
                        }
                    } else {
                        blurredPainter = null
                    }
                }
                
                // Use blurred painter if available, otherwise use original
                val painter = if (backgroundBlur > 0f && blurredPainter != null) {
                    blurredPainter!!
                } else {
                    originalPainter
                }
                
                // Load transform settings from SharedPreferences
                val scale = prefs.getFloat("background_scale_x", 1f)
                val offsetX = prefs.getFloat("background_pos_x", 0f)
                val offsetY = prefs.getFloat("background_pos_y", 0f)
                val rotation = prefs.getFloat("background_rotation", 0f)
                val flipHorizontal = prefs.getBoolean("background_flip_horizontal", false)
                val flipVertical = prefs.getBoolean("background_flip_vertical", false)
                
                Log.d("BackgroundImage", "Transform settings: scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation, flipH=$flipHorizontal, flipV=$flipVertical")
                
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