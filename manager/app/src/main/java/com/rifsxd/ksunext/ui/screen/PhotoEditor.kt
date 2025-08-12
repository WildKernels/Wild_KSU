package com.rifsxd.ksunext.ui.screen

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
import androidx.compose.ui.graphics.ColorMatrix
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

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PhotoEditorScreen(
    imageUri: String,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val uri = Uri.parse(imageUri)
    
    PhotoEditor(
        imageUri = uri,
        onDismiss = {
            navigator.popBackStack()
        },
        onSave = {
            // Save the image URI to preferences
            prefs.edit().putString("background_image_uri", imageUri).apply()
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    // Transform states
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    
    // Image adjustment states
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    
    // UI states
    var freeFormMode by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(false) }
    
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
        ColorMatrix().apply {
            // Apply saturation first
            setToSaturation(saturation)
            
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
            
            // Apply brightness and contrast
            val brightnessOffset = brightness * 255f
            val contrastScale = contrast
            val contrastOffset = (1f - contrast) / 2f * 255f
            
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
        contrast = 1f
        saturation = 1f
        hue = 0f
        flipHorizontal = false
        flipVertical = false
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main photo area (no background, uses default)
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
                .let { modifier ->
                    if (freeFormMode) {
                        modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotationChange ->
                                offsetX += pan.x
                                offsetY += pan.y
                                scale = (scale * zoom).coerceIn(0.1f, 5f)
                                rotation += rotationChange
                            }
                        }
                    } else {
                        modifier
                    }
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )
        
        // Advanced controls (appear above bottom bar when visible)
        if (showControls) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 180.dp), // Position above the bottom bar
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Brightness
                    Text(
                        text = "Brightness: ${(brightness * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -0.5f..0.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Contrast
                    Text(
                        text = "Contrast: ${(contrast * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = 0.5f..2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Saturation
                    Text(
                        text = "Saturation: ${(saturation * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hue (color slider)
                    Text(
                        text = "Hue: ${(hue).toInt()}°",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = -180f..180f,
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
                .padding(bottom = 80.dp), // Extra padding to avoid navigation bar
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Free-form mode toggle (moved from top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Free-form Editing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = freeFormMode,
                        onCheckedChange = { freeFormMode = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate left
                    IconButton(
                        onClick = { rotation -= 90f },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Rotate Left",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Rotate right
                    IconButton(
                        onClick = { rotation += 90f },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate Right",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Flip horizontal
                    IconButton(
                        onClick = { flipHorizontal = !flipHorizontal },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (flipHorizontal) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip Horizontal",
                            tint = if (flipHorizontal) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Flip vertical
                    IconButton(
                        onClick = { flipVertical = !flipVertical },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (flipVertical) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip Vertical",
                            modifier = Modifier.graphicsLayer(rotationZ = 90f),
                            tint = if (flipVertical) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Toggle advanced controls (switched position with reset)
                    IconButton(
                        onClick = { showControls = !showControls },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showControls) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Advanced Controls",
                            tint = if (showControls) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Reset (switched position with menu)
                    IconButton(
                        onClick = { resetAll() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
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
                        onClick = onSave,
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