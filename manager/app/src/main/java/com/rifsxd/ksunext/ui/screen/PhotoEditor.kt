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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState

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
        
        // Get current flip and color states from preferences
        val flipHorizontal = prefs.getBoolean("${imageUri}_flip_horizontal", false)
        val flipVertical = prefs.getBoolean("${imageUri}_flip_vertical", false)
        val brightness = prefs.getFloat("${imageUri}_brightness", 0f)
        val contrast = prefs.getFloat("${imageUri}_contrast", 0f)
        val saturation = prefs.getFloat("${imageUri}_saturation", 0f)
        val hue = prefs.getFloat("${imageUri}_hue", 0f)
        
        println("PhotoEditor: Saving with scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation")
        println("PhotoEditor: Flip states - horizontal=$flipHorizontal, vertical=$flipVertical")
        println("PhotoEditor: Color adjustments - brightness=$brightness, contrast=$contrast, saturation=$saturation, hue=$hue")
        
        prefs.edit()
            .putString("background_image_uri", imageUri)
            .putFloat("background_scale_x", scale)
            .putFloat("background_pos_x", offsetX)
            .putFloat("background_pos_y", offsetY)
            .putFloat("background_rotation", rotation)
            .putFloat("background_transparency", currentTransparency)
            .putString("background_fit_mode", "fit")
            // Save flip states to main background settings
            .putBoolean("background_flip_horizontal", flipHorizontal)
            .putBoolean("background_flip_vertical", flipVertical)
            // Save color adjustments to main background settings
            .putFloat("background_brightness", brightness)
            .putFloat("background_contrast", contrast)
            .putFloat("background_saturation", saturation)
            .putFloat("background_hue", hue)
            .apply()
        println("PhotoEditor: All settings saved, navigating back")
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
            
            // Also load flip and color states from main background settings and copy to image-specific keys
            val flipHorizontal = prefs.getBoolean("background_flip_horizontal", false)
            val flipVertical = prefs.getBoolean("background_flip_vertical", false)
            val brightness = prefs.getFloat("background_brightness", 0f)
            val contrast = prefs.getFloat("background_contrast", 0f)
            val saturation = prefs.getFloat("background_saturation", 0f)
            val hue = prefs.getFloat("background_hue", 0f)
            
            // Copy to image-specific keys for the PhotoEditor to use
            prefs.edit()
                .putBoolean("${imageUri}_flip_horizontal", flipHorizontal)
                .putBoolean("${imageUri}_flip_vertical", flipVertical)
                .putFloat("${imageUri}_brightness", brightness)
                .putFloat("${imageUri}_contrast", contrast)
                .putFloat("${imageUri}_saturation", saturation)
                .putFloat("${imageUri}_hue", hue)
                .apply()
            
            println("PhotoEditor: Loaded existing settings for image: scale=$scale, offsetX=$offsetX, offsetY=$offsetY, rotation=$rotation")
            println("PhotoEditor: Loaded flip states - horizontal=$flipHorizontal, vertical=$flipVertical")
            println("PhotoEditor: Loaded color adjustments - brightness=$brightness, contrast=$contrast, saturation=$saturation, hue=$hue")
        } else {
            // Reset to defaults for new image
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            rotation = 0f
            
            // Reset image-specific settings to defaults
            prefs.edit()
                .putBoolean("${imageUri}_flip_horizontal", false)
                .putBoolean("${imageUri}_flip_vertical", false)
                .putFloat("${imageUri}_brightness", 0f)
                .putFloat("${imageUri}_contrast", 0f)
                .putFloat("${imageUri}_saturation", 0f)
                .putFloat("${imageUri}_hue", 0f)
                .apply()
            
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
        
        // Advanced controls modal bottom sheet
        if (showAdvancedControls) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedControls = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Text(
                        text = "Advanced Controls",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Free-form editing toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Free-form Editing",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = freeFormEditing,
                            onCheckedChange = { freeFormEditing = it }
                        )
                    }
                    
                    // Scale slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scale",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${(currentScale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = currentScale,
                            onValueChange = { newScale ->
                                currentScale = newScale
                                onTransformChange(newScale, currentOffsetX, currentOffsetY, currentRotation)
                            },
                            valueRange = 0.1f..5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                            // Save rotation immediately
                            prefs.edit().putFloat("background_rotation", currentRotation).apply()
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
                            // Save rotation immediately
                            prefs.edit().putFloat("background_rotation", currentRotation).apply()
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
        
        // Color controls modal bottom sheet <mcreference link="https://developer.android.com/develop/ui/compose/components/bottom-sheets" index="2">2</mcreference>
        if (showColorControls) {
            ModalBottomSheet(
                onDismissRequest = { showColorControls = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Text(
                        text = "Color Adjustments",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Brightness slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Brightness",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${brightness.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = brightness,
                            onValueChange = { 
                                brightness = it
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_brightness", it).apply()
                            },
                            valueRange = -100f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Contrast slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Contrast",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${contrast.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = contrast,
                            onValueChange = { 
                                contrast = it
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_contrast", it).apply()
                            },
                            valueRange = -100f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Saturation slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Saturation",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${saturation.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = saturation,
                            onValueChange = { 
                                saturation = it
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_saturation", it).apply()
                            },
                            valueRange = -100f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Hue slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hue",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${hue.toInt()}°",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { 
                                hue = it
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_hue", it).apply()
                            },
                            valueRange = -100f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}