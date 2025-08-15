package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Crop
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
    
    // Save the current background image URI as previous before editing
    LaunchedEffect(imageUri) {
        val currentUri = prefs.getString("background_image_uri", "")
        if (currentUri?.isNotEmpty() == true && currentUri != imageUri) {
            prefs.edit().putString("previous_background_image_uri", currentUri).apply()
        }
    }
    
    val saveFunction = { scale: Float, offsetX: Float, offsetY: Float, rotation: Float ->
        // Save transform settings and background configuration
        // Preserve existing background_transparency setting instead of overriding it
        val currentTransparency = prefs.getFloat("background_transparency", 0.0f)
        
        // Get current flip and color states from image-specific preferences
        val imageUriString = imageUri.toString()
        val flipHorizontal = prefs.getBoolean("${imageUriString}_flip_horizontal", false)
        val flipVertical = prefs.getBoolean("${imageUriString}_flip_vertical", false)
        val brightness = prefs.getFloat("${imageUriString}_brightness", 1.0f)
        val contrast = prefs.getFloat("${imageUriString}_contrast", 1.0f)
        val saturation = prefs.getFloat("${imageUriString}_saturation", 1.0f)
        val hue = prefs.getFloat("${imageUriString}_hue", 0f)
        
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
            val brightness = prefs.getFloat("background_brightness", 1.0f)
            val contrast = prefs.getFloat("background_contrast", 1.0f)
            val saturation = prefs.getFloat("background_saturation", 1.0f)
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
                .putFloat("${imageUri}_brightness", 1.0f)
                .putFloat("${imageUri}_contrast", 1.0f)
                .putFloat("${imageUri}_saturation", 1.0f)
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
            // Restore previous image selection by clearing current selection
            val previousUri = prefs.getString("previous_background_image_uri", "")
            if (previousUri?.isNotEmpty() == true) {
                prefs.edit()
                    .putString("background_image_uri", previousUri)
                    .remove("previous_background_image_uri")
                    .apply()
            } else {
                // If no previous image, clear current selection
                prefs.edit()
                    .remove("background_image_uri")
                    .apply()
            }
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
    var showCropMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(1.0f) }
    var contrast by remember { mutableFloatStateOf(1.0f) }
    var saturation by remember { mutableFloatStateOf(1.0f) }
    var hue by remember { mutableFloatStateOf(0f) }
    var freeFormEditing by remember { mutableStateOf(true) }
    
    // Load existing color settings for this specific image
    LaunchedEffect(imageUri) {
        val imageUriString = imageUri.toString()
        brightness = prefs.getFloat("${imageUriString}_brightness", 1.0f)
        contrast = prefs.getFloat("${imageUriString}_contrast", 1.0f)
        saturation = prefs.getFloat("${imageUriString}_saturation", 1.0f)
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
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply {
                    // Start with identity matrix
                    reset()
                    
                    // Apply brightness (1.0 = normal, 0.0 = black, 2.0 = very bright)
                    if (brightness != 1.0f) {
                        val brightnessMatrix = ColorMatrix(floatArrayOf(
                            1f, 0f, 0f, 0f, (brightness - 1f) * 255f,
                            0f, 1f, 0f, 0f, (brightness - 1f) * 255f,
                            0f, 0f, 1f, 0f, (brightness - 1f) * 255f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        this.timesAssign(brightnessMatrix)
                    }
                    
                    // Apply contrast (1.0 = normal, 0.0 = gray, 2.0 = high contrast)
                    if (contrast != 1.0f) {
                        val contrastMatrix = ColorMatrix(floatArrayOf(
                            contrast, 0f, 0f, 0f, 128f * (1f - contrast),
                            0f, contrast, 0f, 0f, 128f * (1f - contrast),
                            0f, 0f, contrast, 0f, 128f * (1f - contrast),
                            0f, 0f, 0f, 1f, 0f
                        ))
                        this.timesAssign(contrastMatrix)
                    }
                    
                    // Apply saturation (1.0 = normal)
                    if (saturation != 1.0f) {
                        val saturationMatrix = ColorMatrix().apply {
                            setToSaturation(saturation)
                        }
                        this.timesAssign(saturationMatrix)
                    }
                    
                    // Apply hue rotation (0.0 = normal)
                    if (hue != 0f) {
                        val hueMatrix = ColorMatrix()
                        hueMatrix.setToRotateRed(hue)
                        this.timesAssign(hueMatrix)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(freeFormEditing) {
                    if (!freeFormEditing) {
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
                }
                .graphicsLayer(
                    scaleX = currentScale * (if (flipHorizontal) -1f else 1f),
                    scaleY = currentScale * (if (flipVertical) -1f else 1f),
                    translationX = currentOffsetX,
                    translationY = currentOffsetY,
                    rotationZ = currentRotation,
                    transformOrigin = TransformOrigin.Center
                ),

            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
        
        // Removed full-screen overlays to keep photo visible
        

        

        
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
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Inline Crop Menu
                AnimatedVisibility(
                    visible = showCropMenu,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        // Header
                        Text(
                            text = "Crop Tools",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Rotation controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = {
                                    currentRotation = (currentRotation - 90f) % 360f
                                    onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                                    prefs.edit().putFloat("background_rotation", currentRotation).apply()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateLeft,
                                    contentDescription = "Rotate Left"
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    currentRotation = (currentRotation + 90f) % 360f
                                    onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                                    prefs.edit().putFloat("background_rotation", currentRotation).apply()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate Right"
                                )
                            }
                            
                            IconButton(
                                onClick = { 
                                    flipHorizontal = !flipHorizontal
                                    val imageUriString = imageUri.toString()
                                    prefs.edit().putBoolean("${imageUriString}_flip_horizontal", flipHorizontal).apply()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Flip Horizontal",
                                    tint = if (flipHorizontal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            IconButton(
                                onClick = { 
                                    flipVertical = !flipVertical
                                    val imageUriString = imageUri.toString()
                                    prefs.edit().putBoolean("${imageUriString}_flip_vertical", flipVertical).apply()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Flip Vertical",
                                    tint = if (flipVertical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Scale control
                        Text(
                            text = "Scale: ${String.format("%.1f", currentScale)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Slider(
                            value = currentScale,
                            onValueChange = { newScale ->
                                currentScale = newScale
                                onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                                prefs.edit().putFloat("background_scale", currentScale).apply()
                            },
                            valueRange = 0.1f..3.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Position controls
                        Text(
                            text = "Position Controls",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                        )
                        
                        // Horizontal position
                        Text(
                            text = "Left/Right: ${String.format("%.0f", currentOffsetX)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = currentOffsetX,
                            onValueChange = { newOffsetX ->
                                currentOffsetX = newOffsetX
                                onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                                prefs.edit().putFloat("background_pos_x", currentOffsetX).apply()
                            },
                            valueRange = -1000f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Vertical position
                        Text(
                            text = "Up/Down: ${String.format("%.0f", currentOffsetY)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = currentOffsetY,
                            onValueChange = { newOffsetY ->
                                currentOffsetY = newOffsetY
                                onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                                prefs.edit().putFloat("background_pos_y", currentOffsetY).apply()
                            },
                            valueRange = -1000f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Free edit toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Free Edit Mode: ${if (freeFormEditing) "Enabled" else "Disabled"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = freeFormEditing,
                                onCheckedChange = { freeFormEditing = it }
                            )
                        }
                        Text(
                            text = "When enabled, disables touch gestures for precise slider control",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Inline Color Menu
                AnimatedVisibility(
                    visible = showColorMenu,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        // Header
                        Text(
                            text = "Color Adjustments",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Brightness control
                        Text(
                            text = "Brightness: ${String.format("%.1f", brightness)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = brightness,
                            onValueChange = { newBrightness ->
                                brightness = newBrightness
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_brightness", brightness).apply()
                            },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Contrast control
                        Text(
                            text = "Contrast: ${String.format("%.1f", contrast)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = contrast,
                            onValueChange = { newContrast ->
                                contrast = newContrast
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_contrast", contrast).apply()
                            },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Saturation control
                        Text(
                            text = "Saturation: ${String.format("%.1f", saturation)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = saturation,
                            onValueChange = { newSaturation ->
                                saturation = newSaturation
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_saturation", saturation).apply()
                            },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Hue control
                        Text(
                            text = "Hue: ${String.format("%.0f", hue)}°",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = hue,
                            onValueChange = { newHue ->
                                hue = newHue
                                val imageUriString = imageUri.toString()
                                prefs.edit().putFloat("${imageUriString}_hue", hue).apply()
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Control bar with 4 buttons
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceEvenly,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     // Crop button with Material 3 Expressive styling
                     IconButton(
                         onClick = { 
                             if (showCropMenu) {
                                 showCropMenu = false
                             } else {
                                 showColorMenu = false
                                 showCropMenu = true
                             }
                         },
                         modifier = Modifier
                             .size(56.dp)
                             .clip(RoundedCornerShape(16.dp))
                             .background(
                                 if (showCropMenu) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surfaceVariant
                             )
                     ) {
                         Icon(
                             imageVector = Icons.Filled.Crop,
                             contentDescription = "Crop Tools",
                             tint = if (showCropMenu) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.size(24.dp)
                         )
                     }
                     
                     // Color button with Material 3 Expressive styling
                     IconButton(
                         onClick = { 
                             if (showColorMenu) {
                                 showColorMenu = false
                             } else {
                                 showCropMenu = false
                                 showColorMenu = true
                             }
                         },
                         modifier = Modifier
                             .size(56.dp)
                             .clip(RoundedCornerShape(16.dp))
                             .background(
                                 if (showColorMenu) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surfaceVariant
                             )
                     ) {
                         Icon(
                             imageVector = Icons.Default.Palette,
                             contentDescription = "Color Adjustments",
                             tint = if (showColorMenu) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.size(24.dp)
                         )
                     }
                     
                     // Reset button with Material 3 Expressive styling
                     IconButton(
                         onClick = {
                             // Close any open menus
                             showCropMenu = false
                             showColorMenu = false
                             
                             // Reset all settings
                             currentScale = 1.0f
                             currentOffsetX = 0f
                             currentOffsetY = 0f
                             currentRotation = 0f
                             brightness = 1.0f
                             contrast = 1.0f
                             saturation = 1.0f
                             hue = 0f
                             flipHorizontal = false
                             flipVertical = false
                             freeFormEditing = false
                             
                             // Update transformations
                             onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                             
                             // Clear preferences
                             val imageUriString = imageUri.toString()
                             prefs.edit()
                                 .remove("background_scale")
                                 .remove("background_rotation")
                                 .remove("${imageUriString}_brightness")
                                 .remove("${imageUriString}_contrast")
                                 .remove("${imageUriString}_saturation")
                                 .remove("${imageUriString}_hue")
                                 .remove("${imageUriString}_flip_horizontal")
                                 .remove("${imageUriString}_flip_vertical")
                                 .apply()
                         },
                         modifier = Modifier
                             .size(56.dp)
                             .clip(RoundedCornerShape(16.dp))
                             .background(MaterialTheme.colorScheme.surfaceVariant)
                     ) {
                         Icon(
                             imageVector = Icons.Default.Refresh,
                             contentDescription = "Reset All",
                             tint = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.size(24.dp)
                         )
                     }
                     
                     // Confirm button with Material 3 Expressive styling
                     IconButton(
                         onClick = {
                             showCropMenu = false
                             showColorMenu = false
                             onSave()
                         },
                         modifier = Modifier
                             .size(56.dp)
                             .clip(RoundedCornerShape(16.dp))
                             .background(MaterialTheme.colorScheme.primary)
                     ) {
                         Icon(
                             imageVector = Icons.Default.Check,
                             contentDescription = "Confirm",
                             tint = MaterialTheme.colorScheme.onPrimary,
                             modifier = Modifier.size(24.dp)
                         )
                     }
                 }
            }
        }
    }
}