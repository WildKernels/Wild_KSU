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
import coil.compose.rememberAsyncImagePainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ImageCropSettings
import com.rifsxd.ksunext.ui.util.ImageCropUtils

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
    
    PhotoEditor(
        imageUri = uri,
        onDismiss = {
            navigator.popBackStack()
        },
        onSave = { scale, offsetX, offsetY, rotation ->
            // Save image URI and reset transparency (like original AdvancedImageCropDialog)
            prefs.edit()
                .putString("background_image_uri", imageUri)
                .putFloat("background_transparency", 0.0f) // Reset darkness so image is visible
                .apply()
            
            // Save crop settings for graphicsLayer transformations
            val cropSettings = ImageCropSettings(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                rotation = rotation
            )
            ImageCropUtils.saveImageCropSettings(prefs, imageUri, cropSettings)
            
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float) -> Unit // scale, offsetX, offsetY, rotation
) {
    // Transform states - simple like original AdvancedImageCropDialog
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // UI state
    var freeFormMode by remember { mutableStateOf(true) }
    
    // Simple image painter without custom decoders
    val painter = rememberAsyncImagePainter(
        model = imageUri,
        onError = { error ->
            android.util.Log.e("PhotoEditor", "Failed to load image: ${error.result.throwable?.message}")
        }
    )
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main image display with graphicsLayer transformations (like original)
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
            contentScale = ContentScale.Fit
        )
        
        // Top bar with close and save buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Save button
            IconButton(
                onClick = { onSave(scale, offsetX, offsetY, rotation) },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Bottom controls
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
                
                // Transform controls
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
                    
                    // Reset
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            rotation = 0f
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
}