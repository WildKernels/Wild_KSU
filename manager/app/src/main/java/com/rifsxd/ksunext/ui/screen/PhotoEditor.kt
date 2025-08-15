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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Palette
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
    
    // Load existing settings for this specific image or use defaults
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Load existing settings for this specific image
    LaunchedEffect(imageUri) {
        val existingUri = prefs.getString("background_image_uri", "")
        if (existingUri == imageUri) {
            // Load existing transform settings for this image
            scale = prefs.getFloat("background_scale_x", 1f)
            offsetX = prefs.getFloat("background_pos_x", 0f)
            offsetY = prefs.getFloat("background_pos_y", 0f)
            rotation = prefs.getFloat("background_rotation", 0f)
            println("PhotoEditor: Loaded existing settings for image: scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation")
        } else {
            // Reset to defaults for new image
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            rotation = 0f
            println("PhotoEditor: New image, using default transform settings")
        }
    }
    
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
    var showColorControls by remember { mutableStateOf(false) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var freeFormEditing by remember { mutableStateOf(false) }
    
    // Load existing color settings for this specific image
    LaunchedEffect(imageUri) {
        val imageUriString = imageUri.toString()
        brightness = prefs.getFloat("${imageUriString}_brightness", 0f)
        contrast = prefs.getFloat("${imageUriString}_contrast", 0f)
        saturation = prefs.getFloat("${imageUriString}_saturation", 0f)
        hue = prefs.getFloat("${imageUriString}_hue", 0f)
        flipHorizontal = prefs.getBoolean("${imageUriString}_flip_horizontal", false)
        flipVertical = prefs.getBoolean("${imageUriString}_flip_vertical", false)
    }
    
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
                    scaleX = currentScale * (if (flipHorizontal) -1f else 1f),
                    scaleY = currentScale * (if (flipVertical) -1f else 1f),
                    translationX = currentOffsetX,
                    translationY = currentOffsetY,
                    rotationZ = currentRotation,
                    transformOrigin = TransformOrigin.Center,
                    alpha = (1f + (brightness / 200f)).coerceIn(0f, 1f)
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
                    .padding(bottom = 200.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                }
            }
        }
        
        // Color controls panel (when enabled)
         if (showColorControls) {
             Card(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .fillMaxWidth()
                     .padding(bottom = 200.dp)
                     .padding(horizontal = 16.dp),
                 colors = CardDefaults.cardColors(
                     containerColor = MaterialTheme.colorScheme.surfaceContainer
                 )
             ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Brightness slider
                    Text("Brightness: ${brightness.toInt()}")
                    Slider(
                        value = brightness,
                        onValueChange = { 
                            brightness = it
                            val imageUriString = imageUri.toString()
                            prefs.edit().putFloat("${imageUriString}_brightness", it).apply()
                        },
                        valueRange = -100f..100f
                    )
                    
                    // Contrast slider
                    Text("Contrast: ${contrast.toInt()}")
                    Slider(
                        value = contrast,
                        onValueChange = { 
                            contrast = it
                            val imageUriString = imageUri.toString()
                            prefs.edit().putFloat("${imageUriString}_contrast", it).apply()
                        },
                        valueRange = -100f..100f
                    )
                    
                    // Saturation slider
                    Text("Saturation: ${saturation.toInt()}")
                    Slider(
                        value = saturation,
                        onValueChange = { 
                            saturation = it
                            val imageUriString = imageUri.toString()
                            prefs.edit().putFloat("${imageUriString}_saturation", it).apply()
                        },
                        valueRange = -100f..100f
                    )
                    
                    // Hue slider
                    Text("Hue: ${hue.toInt()}°")
                    Slider(
                        value = hue,
                        onValueChange = { 
                            hue = it
                            val imageUriString = imageUri.toString()
                            prefs.edit().putFloat("${imageUriString}_hue", it).apply()
                        },
                        valueRange = -100f..100f
                    )
                }
            }
        }
        
        // Bottom controls overlay - positioned as a separate layer
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(16.dp)
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
                        onClick = { 
                            flipHorizontal = !flipHorizontal
                            val imageUriString = imageUri.toString()
                            prefs.edit().putBoolean("${imageUriString}_flip_horizontal", flipHorizontal).apply()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Flip Horizontal"
                        )
                    }
                    
                    // Flip vertical
                    IconButton(
                        onClick = { 
                            flipVertical = !flipVertical
                            val imageUriString = imageUri.toString()
                            prefs.edit().putBoolean("${imageUriString}_flip_vertical", flipVertical).apply()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Flip Vertical"
                        )
                    }
                    
                    // Color controls toggle
                    IconButton(
                        onClick = { showColorControls = !showColorControls },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showColorControls) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Color Controls",
                            tint = if (showColorControls) MaterialTheme.colorScheme.onPrimary
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
                            
                            // Reset color settings in preferences
                            val imageUriString = imageUri.toString()
                            prefs.edit()
                                .putFloat("${imageUriString}_brightness", 0f)
                                .putFloat("${imageUriString}_contrast", 0f)
                                .putFloat("${imageUriString}_saturation", 0f)
                                .putFloat("${imageUriString}_hue", 0f)
                                .putBoolean("${imageUriString}_flip_horizontal", false)
                                .putBoolean("${imageUriString}_flip_vertical", false)
                                .apply()
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