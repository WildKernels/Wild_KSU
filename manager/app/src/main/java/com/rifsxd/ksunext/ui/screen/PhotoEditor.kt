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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
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
import com.rifsxd.ksunext.R
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
    
    // Create mutable state to hold the save function
    var saveFunction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    CompositionLocalProvider(
        LocalPhotoEditorSave provides saveFunction
    ) {
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
                    .putString("background_fit_mode", "custom_crop") // Ensure custom crop mode is set
                    .apply()
                
                // Save all transform and adjustment settings using ImageTransformSettings
                val allSettings = ImageTransformSettings(
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    rotation = rotation,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    hue = hue
                )
                ImageTransformSettings.saveToPrefs(prefs, allSettings)
                BackgroundEditorUtils.saveImageTransformSettings(prefs, imageUri, allSettings)
                
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
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Load saved transform settings or use defaults
    val savedSettings = remember { ImageTransformSettings.loadFromPrefs(prefs) }
    
    // Transform states - load from saved settings
    var offsetX by remember { mutableFloatStateOf(savedSettings.offsetX) }
    var offsetY by remember { mutableFloatStateOf(savedSettings.offsetY) }
    var scale by remember { mutableFloatStateOf(savedSettings.scale) }
    var rotation by remember { mutableFloatStateOf(savedSettings.rotation) }
    
    // Image adjustment states with expanded ranges for better effects - load from saved settings
    var brightness by remember { mutableFloatStateOf(savedSettings.brightness) } // -200 to 200 (expanded from -100 to 100)
    var contrast by remember { mutableFloatStateOf(savedSettings.contrast) } // 0 to 4 (expanded from 0 to 2)
    var saturation by remember { mutableFloatStateOf(savedSettings.saturation) } // 0 to 3 (expanded from 0 to 2)
    var hue by remember { mutableFloatStateOf(savedSettings.hue) } // -360 to 360 (expanded from -180 to 180)
    
    // UI state
    var freeFormMode by remember { mutableStateOf(true) }
    var selectedControl by remember { mutableStateOf("None") }
    var showDropdown by remember { mutableStateOf(false) }
    
    val controlOptions = listOf(
        "None",
        "Position X",
        "Position Y", 
        "Rotation",
        "Scale",
        "Brightness",
        "Contrast",
        "Saturation",
        "Hue"
    )
    
    // Provide save function that captures current state values
    LaunchedEffect(scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue) {
        val saveFunc = {
            onSave(scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue)
        }
        onProvideSaveFunction?.invoke(saveFunc)
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main image display - full screen, ignoring all bars
        Image(
            painter = painter,
            contentDescription = "Photo to edit",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotationChange ->
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offsetX = (offsetX + pan.x).coerceIn(-1000f, 1000f)
                        offsetY = (offsetY + pan.y).coerceIn(-1000f, 1000f)
                        rotation = (rotation + rotationChange) % 360f
                    }
                }
                .graphicsLayer(
                    scaleX = BackgroundEditorUtils.constrainScale(scale),
                    scaleY = BackgroundEditorUtils.constrainScale(scale),
                    translationX = BackgroundEditorUtils.constrainTranslation(offsetX),
                    translationY = BackgroundEditorUtils.constrainTranslation(offsetY),
                    rotationZ = BackgroundEditorUtils.constrainRotation(rotation),
                    transformOrigin = TransformOrigin.Center
                ),
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.Center,
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )
        
        // Bottom controls - positioned absolutely at bottom
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
                // Main button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Control selector dropdown
                    ExposedDropdownMenuBox(
                        expanded = showDropdown,
                        onExpandedChange = { showDropdown = !showDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedControl,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Control") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            controlOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedControl = option
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
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
                    
                    // Confirm button
                    IconButton(
                        onClick = {
                            onSave(scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // Show selected control slider
                if (selectedControl != "None") {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedControl) {
                        "Position X" -> {
                            Column {
                                Text(
                                    text = "Position X: ${offsetX.toInt()}px",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = offsetX,
                                    onValueChange = { offsetX = it },
                                    valueRange = -1000f..1000f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Position Y" -> {
                            Column {
                                Text(
                                    text = "Position Y: ${offsetY.toInt()}px",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = offsetY,
                                    onValueChange = { offsetY = it },
                                    valueRange = -1000f..1000f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Rotation" -> {
                            Column {
                                Text(
                                    text = "Rotation: ${rotation.toInt()}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = rotation,
                                    onValueChange = { rotation = it },
                                    valueRange = -360f..360f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Quick rotation buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(
                                        onClick = { rotation -= 90f },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RotateLeft,
                                            contentDescription = "Rotate Left 90°",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { rotation += 90f },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RotateRight,
                                            contentDescription = "Rotate Right 90°",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        "Scale" -> {
                            Column {
                                Text(
                                    text = "Scale: ${String.format("%.1f", scale)}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = scale,
                                    onValueChange = { scale = it },
                                    valueRange = 0.1f..5f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Brightness" -> {
                            Column {
                                Text(
                                    text = "Brightness: ${brightness.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -200f..200f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Contrast" -> {
                            Column {
                                Text(
                                    text = "Contrast: ${String.format("%.1f", contrast)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0f..4f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Saturation" -> {
                            Column {
                                Text(
                                    text = "Saturation: ${String.format("%.1f", saturation)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0f..3f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Hue" -> {
                            Column {
                                Text(
                                    text = "Hue: ${hue.toInt()}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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