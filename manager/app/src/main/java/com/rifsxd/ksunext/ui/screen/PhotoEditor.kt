package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    val saveFunction = { scale: Float, offsetX: Float, offsetY: Float, rotation: Float ->
        // Save transform settings and background configuration
        // Preserve existing background_transparency setting instead of overriding it
        val currentTransparency = prefs.getFloat("background_transparency", 0.0f)
        println("PhotoEditor: Saving with scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation")
        prefs.edit()
            .putString("background_image_uri", imageUri)
            .putFloat("background_scale_x", scale)
            .putFloat("background_pos_x", offsetX)
            .putFloat("background_pos_y", offsetY)
            .putFloat("background_rotation", rotation)
            .putFloat("background_transparency", currentTransparency)
            .putString("background_fit_mode", "fit")
            .apply()
        println("PhotoEditor: Settings saved, navigating back")
        navigator.popBackStack()
        Unit
    }
    
    // Reset transform states for new image to avoid applying previous image's transforms
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    PhotoEditor(
        imageUri = Uri.parse(imageUri),
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
        rotation = rotation,
        onTransformChange = { newScale, newOffsetX, newOffsetY, newRotation ->
            scale = newScale
            offsetX = newOffsetX
            offsetY = newOffsetY
            rotation = newRotation
        },
        onSave = {
            saveFunction(scale, offsetX, offsetY, rotation)
        },
        onCancel = {
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Float,
    onTransformChange: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Use local mutable state for gesture handling
    var currentScale by remember { mutableFloatStateOf(scale) }
    var currentOffsetX by remember { mutableFloatStateOf(offsetX) }
    var currentOffsetY by remember { mutableFloatStateOf(offsetY) }
    var currentRotation by remember { mutableFloatStateOf(rotation) }
    
    // Additional states for advanced controls
    var showAdvancedControls by remember { mutableStateOf(false) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var freeFormEditing by remember { mutableStateOf(false) }
    
    // Update local state when props change
    LaunchedEffect(scale, offsetX, offsetY, rotation) {
        currentScale = scale
        currentOffsetX = offsetX
        currentOffsetY = offsetY
        currentRotation = rotation
        println("PhotoEditor: Loaded transform settings: scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation")
    }
        // Load image with ImageRequest for consistency
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(false)
                .build()
        )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main image display with touch gestures
        Image(
            painter = painter,
            contentDescription = "Photo to edit",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotationChange ->
                        val newScale = (currentScale * zoom).coerceIn(0.1f, 5f)
                        val newOffsetX = (currentOffsetX + pan.x).coerceIn(-1000f, 1000f)
                        val newOffsetY = (currentOffsetY + pan.y).coerceIn(-1000f, 1000f)
                        val newRotation = (currentRotation + rotationChange) % 360f
                        
                        // Update local state
                        currentScale = newScale
                        currentOffsetX = newOffsetX
                        currentOffsetY = newOffsetY
                        currentRotation = newRotation
                        
                        // Notify parent of transform changes
                        onTransformChange(newScale, newOffsetX, newOffsetY, newRotation)
                        
                        // Save to preferences immediately for real-time updates
                        println("PhotoEditor: Gesture update - scale=$newScale, offsetX=$newOffsetX, offsetY=$newOffsetY, rotation=$newRotation")
                        prefs.edit()
                            .putFloat("background_scale_x", newScale)
                            .putFloat("background_pos_x", newOffsetX)
                            .putFloat("background_pos_y", newOffsetY)
                            .putFloat("background_rotation", newRotation)
                            .apply()
                        Unit
                    }
                }
                .graphicsLayer(
                    scaleX = currentScale,
                    scaleY = currentScale,
                    translationX = currentOffsetX,
                    translationY = currentOffsetY,
                    rotationZ = currentRotation,
                    transformOrigin = TransformOrigin.Center
                ),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
        
        // Advanced controls panel (when enabled)
        if (showAdvancedControls) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 120.dp), // Above the bottom bar
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Free-form editing toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Free-form Editing")
                        Switch(
                            checked = freeFormEditing,
                            onCheckedChange = { freeFormEditing = it }
                        )
                    }
                    
                    // Scale slider
                    Text("Scale: ${(currentScale * 100).toInt()}%")
                    Slider(
                        value = currentScale,
                        onValueChange = { newScale ->
                            currentScale = newScale
                            onTransformChange(newScale, currentOffsetX, currentOffsetY, currentRotation)
                        },
                        valueRange = 0.1f..5f
                    )
                    
                    // Brightness slider
                    Text("Brightness: ${brightness.toInt()}")
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -100f..100f
                    )
                    
                    // Contrast slider
                    Text("Contrast: ${contrast.toInt()}")
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = -100f..100f
                    )
                    
                    // Saturation slider
                    Text("Saturation: ${saturation.toInt()}")
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = -100f..100f
                    )
                    
                    // Hue slider
                    Text("Hue: ${hue.toInt()}°")
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = -100f..100f
                    )
                }
            }
        }
        
        // Bottom controls overlay - positioned as a separate layer
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 16.dp,
            windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate left
                    IconButton(
                        onClick = {
                            currentRotation = (currentRotation - 90f) % 360f
                            onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Rotate Left"
                        )
                    }
                    
                    // Rotate right
                    IconButton(
                        onClick = {
                            currentRotation = (currentRotation + 90f) % 360f
                            onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate Right"
                        )
                    }
                    
                    // Flip horizontal
                    IconButton(
                        onClick = { flipHorizontal = !flipHorizontal },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (flipHorizontal) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipToBack,
                            contentDescription = "Flip Horizontal",
                            tint = if (flipHorizontal) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Flip vertical
                    IconButton(
                        onClick = { flipVertical = !flipVertical },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (flipVertical) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipToBack,
                            contentDescription = "Flip Vertical",
                            tint = if (flipVertical) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Advanced controls toggle
                    IconButton(
                        onClick = { showAdvancedControls = !showAdvancedControls },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showAdvancedControls) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Advanced Controls",
                            tint = if (showAdvancedControls) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Reset button
                    IconButton(
                        onClick = {
                            currentScale = 1f
                            currentOffsetX = 0f
                            currentOffsetY = 0f
                            currentRotation = 0f
                            flipHorizontal = false
                            flipVertical = false
                            brightness = 0f
                            contrast = 0f
                            saturation = 0f
                            hue = 0f
                            onTransformChange(1f, 0f, 0f, 0f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                }
                
                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Cancel button
                     OutlinedButton(
                         onClick = onCancel,
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