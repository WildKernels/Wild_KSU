package com.rifsxd.ksunext.ui.screen

import android.content.Context
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import coil.compose.rememberAsyncImagePainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.util.ImageTransformSettings
import com.rifsxd.ksunext.ui.util.BackgroundEditorUtils

// CompositionLocal for sharing save function with top bar
val LocalPhotoEditorSave = compositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PhotoEditorScreen(
    imageUri: String,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val uri = Uri.parse(imageUri)
    
    // Create a mutable state to hold the save function
    var saveFunction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    CompositionLocalProvider(LocalPhotoEditorSave provides saveFunction) {
        PhotoEditor(
            imageUri = uri,
            onDismiss = {
                navigator.popBackStack()
            },
            onSave = { scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue ->
                // Clear any previous photo settings first
                BackgroundEditorUtils.clearImageTransformSettings(prefs)
                
                // Save image URI and reset transparency (like original AdvancedImageTransformDialog)
                prefs.edit()
                    .putString("background_image_uri", imageUri)
                    .putFloat("background_transparency", 0.0f) // Reset darkness so image is visible
                    .apply()
                
                // Save transform settings for graphicsLayer transformations
                val transformSettings = ImageTransformSettings(
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    rotation = rotation
                )
                BackgroundEditorUtils.saveImageTransformSettings(prefs, imageUri, transformSettings)
                
                // Save adjustment settings
                prefs.edit()
                    .putFloat("image_brightness", brightness)
                    .putFloat("image_contrast", contrast)
                    .putFloat("image_saturation", saturation)
                    .putFloat("image_hue", hue)
                    .apply()
                
                navigator.popBackStack()
            },
            onProvideSaveFunction = { saveFunc ->
                saveFunction = saveFunc
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float, Float, Float, Float, Float) -> Unit, // scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue
    onProvideSaveFunction: (((() -> Unit)) -> Unit)? = null // Callback to provide save function to parent
) {
    // Transform states - simple like original AdvancedImageTransformDialog
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Image adjustment states with expanded ranges for better effects
    var brightness by remember { mutableFloatStateOf(0f) } // -200 to 200 (expanded from -100 to 100)
    var contrast by remember { mutableFloatStateOf(1f) } // 0 to 4 (expanded from 0 to 2)
    var saturation by remember { mutableFloatStateOf(1f) } // 0 to 3 (expanded from 0 to 2)
    var hue by remember { mutableFloatStateOf(0f) } // -360 to 360 (expanded from -180 to 180)
    
    // UI state
    var freeFormMode by remember { mutableStateOf(true) }
    var showCropMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var hideControls by remember { mutableStateOf(false) }
    
    // Provide save function to parent
    LaunchedEffect(Unit) {
        onProvideSaveFunction?.invoke {
            onSave(scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue)
        }
    }
    
    // Simple image painter without custom decoders
    val painter = rememberAsyncImagePainter(
        model = imageUri,
        onError = { error ->
            android.util.Log.e("PhotoEditor", "Failed to load image: ${error.result.throwable?.message}")
        }
    )
    
    // Create color matrix for adjustments
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
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        
        // Show Controls FAB (only show when controls are hidden)
        if (hideControls) {
            FloatingActionButton(
                onClick = { hideControls = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Show Controls",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Main image display with graphicsLayer transformations and color adjustments
        Image(
            painter = painter,
            contentDescription = "Photo to edit",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotation,
                    transformOrigin = TransformOrigin.Center
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotationChange ->
                        if (freeFormMode) {
                            // Apply transformations directly to state variables
                            val normalizedPanX = pan.x / scale
                            val normalizedPanY = pan.y / scale
                            offsetX += normalizedPanX
                            offsetY += normalizedPanY
                            rotation += rotationChange
                        }
                        // Always allow zoom
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                    }
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )
        
        // Bottom controls (only show if not hidden)
        if (!hideControls) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Main button row - 4 buttons as requested
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crop menu button (renamed from rotation)
                        IconButton(
                            onClick = { 
                                showCropMenu = !showCropMenu
                                showColorMenu = false
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showCropMenu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop Menu",
                                tint = if (showCropMenu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Color menu button
                        IconButton(
                            onClick = { 
                                showColorMenu = !showColorMenu
                                showCropMenu = false
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showColorMenu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Color Menu",
                                tint = if (showColorMenu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Reset button
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                rotation = 0f
                                brightness = 0f
                                contrast = 1f
                                saturation = 1f
                                hue = 0f
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Hide controls button
                        IconButton(
                            onClick = { hideControls = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hide Controls",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                
                    // Crop menu (expandable) - renamed from rotation menu
                if (showCropMenu) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Crop Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Free-form mode toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Free Transform",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = freeFormMode,
                                    onCheckedChange = { freeFormMode = it }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Rotation controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Rotate Left
                                IconButton(
                                    onClick = { rotation -= 90f },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateLeft,
                                        contentDescription = "Rotate Left",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Rotate Right
                                IconButton(
                                    onClick = { rotation += 90f },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateRight,
                                        contentDescription = "Rotate Right",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Scale slider
                            Column {
                                Text(
                                    text = "Scale: ${String.format("%.1f", scale)}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = scale,
                                    onValueChange = { scale = it },
                                    valueRange = 0.1f..5f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                
                // Color menu (expandable)
                if (showColorMenu) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Color Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Brightness slider (-200 to 200, expanded range)
                            Column {
                                Text(
                                    text = "Brightness: ${brightness.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -200f..200f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Contrast slider (0 to 4, expanded range)
                            Column {
                                Text(
                                    text = "Contrast: ${String.format("%.1f", contrast)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0f..4f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Saturation slider (0 to 3, expanded range)
                            Column {
                                Text(
                                    text = "Saturation: ${String.format("%.1f", saturation)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0f..3f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Hue slider (-360 to 360, expanded range)
                            Column {
                                Text(
                                    text = "Hue: ${hue.toInt()}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = hue,
                                    onValueChange = { hue = it },
                                    valueRange = -360f..360f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}