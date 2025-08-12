package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.util.ImageStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

// Function to save edited image with transformations
suspend fun saveEditedImage(
    context: Context,
    originalUri: Uri,
    offsetX: Float,
    offsetY: Float,
    rotation: Float,
    scale: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    hue: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean
): String? {
    return withContext(Dispatchers.IO) {
        try {
            // Load the original bitmap
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(originalUri)
                .build()
            
            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return@withContext null
            
            val originalBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: return@withContext null
            
            // Create a new bitmap with transformations applied
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            // Create transformation matrix
            val matrix = Matrix().apply {
                // Apply scale
                postScale(
                    scale * (if (flipHorizontal) -1f else 1f),
                    scale * (if (flipVertical) -1f else 1f),
                    width / 2f,
                    height / 2f
                )
                // Apply rotation
                postRotate(rotation, width / 2f, height / 2f)
                // Apply translation
                postTranslate(offsetX, offsetY)
            }
            
            // Create color matrix for adjustments
            val colorMatrix = android.graphics.ColorMatrix().apply {
                // Brightness
                val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, brightness * 255f,
                    0f, 1f, 0f, 0f, brightness * 255f,
                    0f, 0f, 1f, 0f, brightness * 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
                postConcat(brightnessMatrix)
                
                // Contrast
                val contrastValue = contrast + 1f
                val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    contrastValue, 0f, 0f, 0f, 0f,
                    0f, contrastValue, 0f, 0f, 0f,
                    0f, 0f, contrastValue, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                postConcat(contrastMatrix)
                
                // Saturation
                setSaturation(saturation + 1f)
            }
            
            // Create new bitmap with transformations
            val transformedBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(transformedBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            
            canvas.drawBitmap(originalBitmap, matrix, paint)
            
            // Save to internal storage
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val fileName = "background_edited_${System.currentTimeMillis()}.png"
            val file = File(imagesDir, fileName)
            
            FileOutputStream(file).use { out ->
                transformedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Clean up
            transformedBitmap.recycle()
            
            // Return file URI
            ImageStorageUtils.filePathToUri(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("PhotoEditor", "Failed to save edited image", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PhotoEditorScreen(
    imageUri: String,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val uri = Uri.parse(imageUri)
    val coroutineScope = rememberCoroutineScope()
    
    PhotoEditor(
        imageUri = uri,
        onDismiss = {
            navigator.popBackStack()
        },
        onSave = { offsetX, offsetY, rotation, scale, brightness, contrast, saturation, hue, flipHorizontal, flipVertical ->
            coroutineScope.launch {
                val editedImageUri = saveEditedImage(
                    context = context,
                    originalUri = uri,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    rotation = rotation,
                    scale = scale,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    hue = hue,
                    flipHorizontal = flipHorizontal,
                    flipVertical = flipVertical
                )
                
                if (editedImageUri != null) {
                    // Clean up old background image
                    ImageStorageUtils.deleteInternalBackgroundImage(context)
                    // Save the edited image URI as background
                    prefs.edit().putString("background_image_uri", editedImageUri).apply()
                } else {
                    // Fallback: save original URI if editing failed
                    prefs.edit().putString("background_image_uri", imageUri).apply()
                }
                
                navigator.popBackStack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float, Float, Float, Float, Float, Boolean, Boolean) -> Unit
) {
    // Transform states
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    
    // Image adjustment states
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    
    // UI states
    var freeFormMode by remember { mutableStateOf(true) }
    var showAdjustments by remember { mutableStateOf(false) }
    var showTransforms by remember { mutableStateOf(false) }
    
    val painter = rememberAsyncImagePainter(
        model = imageUri,
        onError = { error ->
            android.util.Log.e("PhotoEditor", "Failed to load image URI: $imageUri, Error: ${error.result.throwable?.message}")
        },
        onSuccess = { 
            android.util.Log.d("PhotoEditor", "Image loaded successfully from URI: $imageUri")
        }
    )
    
    // Log the imageUri for debugging
    LaunchedEffect(imageUri) {
        android.util.Log.d("PhotoEditor", "Screen opened with imageUri: $imageUri")
    }
    
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
                
                // Apply hue matrix to current values
                val currentValues = this.values.copyOf()
                for (i in 0 until 4) {
                    for (j in 0 until 5) {
                        var sum = 0f
                        for (k in 0 until 4) {
                            sum += hueMatrix[i * 5 + k] * currentValues[k * 5 + j]
                        }
                        if (j == 4) sum += hueMatrix[i * 5 + 4]
                        this.values[i * 5 + j] = sum
                    }
                }
            }
            
            // Apply brightness and contrast (convert from -100/100 range)
            val brightnessOffset = (brightness / 100f) * 255f
            val contrastScale = (contrast + 100f) / 100f
            val contrastOffset = (1f - contrastScale) / 2f * 255f
            
            // Manually adjust the matrix values for brightness and contrast
            // The ColorMatrix is a 4x5 matrix stored as a 20-element array
            val values = this.values
            
            // Apply contrast scaling to RGB channels
            values[0] *= contrastScale  // Red scale
            values[6] *= contrastScale  // Green scale  
            values[12] *= contrastScale // Blue scale
            
            // Apply brightness and contrast offset to RGB channels
            values[4] += brightnessOffset + contrastOffset   // Red offset
            values[9] += brightnessOffset + contrastOffset   // Green offset
            values[14] += brightnessOffset + contrastOffset  // Blue offset
        }
    }
    
    // Reset function
    fun resetAll() {
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        scale = 1f
        brightness = 0f
        contrast = 0f
        saturation = 0f
        hue = 0f
        flipHorizontal = false
        flipVertical = false
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main photo area (no background applied to screen)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painter,
                contentDescription = "Photo to edit",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation,
                        scaleX = scale * (if (flipHorizontal) -1f else 1f),
                        scaleY = scale * (if (flipVertical) -1f else 1f)
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            // Always allow zoom regardless of free-form mode
                            val newScale = (scale * zoom).coerceIn(0.1f, 5f)
                            scale = newScale
                            
                            if (freeFormMode) {
                                // Normalize pan by current scale to maintain consistent drag speed
                                // This ensures that dragging feels the same regardless of zoom level
                                val normalizedPanX = pan.x / scale
                                val normalizedPanY = pan.y / scale
                                offsetX += normalizedPanX
                                offsetY += normalizedPanY
                                
                                // Apply rotation
                                rotation += rotationChange
                            }
                        }
                    },
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        }
        
        // Transform controls (scale, rotate, flip)
        if (showTransforms) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 140.dp), // Positioned higher to be fully visible
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Transform",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Scale
                    Text(
                        text = "Scale: ${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.1f..5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Rotation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { rotation -= 90f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateLeft,
                                contentDescription = "Rotate Left",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate Left")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = { rotation += 90f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate Right",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate Right")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Flip buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { flipHorizontal = !flipHorizontal },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (flipHorizontal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip Horizontal",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip H")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = { flipVertical = !flipVertical },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (flipVertical) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip Vertical",
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer(rotationZ = 90f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip V")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Free-form mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Free-form Editing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = freeFormMode,
                            onCheckedChange = { freeFormMode = it }
                        )
                    }
                }
            }
        }
        
        // Adjustment controls (brightness, contrast, saturation, hue)
        if (showAdjustments) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 140.dp), // Positioned higher to be fully visible
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Brightness
                    Text(
                        text = "Brightness: ${brightness.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Contrast
                    Text(
                        text = "Contrast: ${contrast.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Saturation
                    Text(
                        text = "Saturation: ${saturation.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hue (color slider)
                    Text(
                        text = "Hue: ${hue.toInt()}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Bottom controls and action bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 16.dp), // Moved closer to bottom
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                
                // Menu buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Transform menu (scale, rotate, flip)
                    IconButton(
                        onClick = { 
                            showTransforms = !showTransforms
                            if (showTransforms) showAdjustments = false
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showTransforms) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropRotate,
                            contentDescription = "Transform",
                            tint = if (showTransforms) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Adjustments menu (brightness, contrast, etc.)
                    IconButton(
                        onClick = { 
                            showAdjustments = !showAdjustments
                            if (showAdjustments) showTransforms = false
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showAdjustments) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Adjustments",
                            tint = if (showAdjustments) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Reset all
                    IconButton(
                        onClick = { resetAll() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset All",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Confirm button
                    Button(
                        onClick = {
                            onSave(
                                offsetX, offsetY, rotation, scale,
                                brightness, contrast, saturation, hue,
                                flipHorizontal, flipVertical
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}