package com.rifsxd.ksunext.ui.component

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.rifsxd.ksunext.ui.util.ImageCropUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedImageCropDialog(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    
    // Transformation states - scale, position, and rotation
    var scale by remember { mutableFloatStateOf(prefs.getFloat("background_scale_x", 1.0f)) }
    var offsetX by remember { mutableFloatStateOf(prefs.getFloat("background_pos_x", 0.0f)) }
    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("background_pos_y", 0.0f)) }
    var rotation by remember { mutableFloatStateOf(prefs.getFloat("background_rotation", 0.0f)) }
    
    // Get current transparency for preview only - default to 0.0f (solid)
    val backgroundTransparency = prefs.getFloat("background_transparency", 0.0f)
    
    val painter = rememberAsyncImagePainter(
        model = imageUri,
        onError = { error ->
            android.util.Log.e("AdvancedImageCropDialog", "Failed to load image URI: $imageUri, Error: ${error.result.throwable?.message}")
        },
        onSuccess = { 
            android.util.Log.d("AdvancedImageCropDialog", "Image loaded successfully from URI: $imageUri")
        }
    )
    
    // Log the imageUri for debugging
    LaunchedEffect(imageUri) {
        android.util.Log.d("AdvancedImageCropDialog", "Dialog opened with imageUri: $imageUri")
    }
    val (minScale, maxScale) = ImageCropUtils.getScaleLimits()
    val (minTranslation, maxTranslation) = ImageCropUtils.getTranslationLimits()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image preview with full transformations (zoom, drag, and rotation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotationDelta ->
                            // Apply zoom
                            scale = (scale * zoom).coerceIn(minScale, maxScale)
                            // Apply pan/drag
                            offsetX = (offsetX + pan.x).coerceIn(minTranslation, maxTranslation)
                            offsetY = (offsetY + pan.y).coerceIn(minTranslation, maxTranslation)
                            // Apply rotation with finger drag
                            rotation = (rotation + rotationDelta) % 360f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Background Image Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = rotation
                        ),
                    contentScale = ContentScale.FillBounds
                )
                
                // Template overlay showing where UI elements will be
                UITemplateOverlay(backgroundTransparency = backgroundTransparency)
            }
            
            // Enhanced bottom controls with rotation and auto-fit
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.8f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(16.dp)
            ) {
                // Auto-fit button (centered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Auto-fit button - properly fills screen without black bars
                    OutlinedButton(
                        onClick = {
                            // Auto fit - center and scale to fit screen properly
                            scale = 1.0f // Use 1.0f since FillBounds will handle stretching
                            offsetX = 0.0f
                            offsetY = 0.0f
                            rotation = 0.0f
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto Fit")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Second row - Reset and Confirm buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset to original saved values or defaults
                            scale = prefs.getFloat("background_scale_x", 1.0f)
                            offsetX = prefs.getFloat("background_pos_x", 0.0f)
                            offsetY = prefs.getFloat("background_pos_y", 0.0f)
                            rotation = prefs.getFloat("background_rotation", 0.0f)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Save all transformations including rotation
                            ImageCropUtils.saveConstrainedScale(prefs, "background_scale_x", scale)
                            ImageCropUtils.saveConstrainedScale(prefs, "background_scale_y", scale)
                            ImageCropUtils.saveConstrainedTranslation(prefs, "background_pos_x", offsetX)
                            ImageCropUtils.saveConstrainedTranslation(prefs, "background_pos_y", offsetY)
                            ImageCropUtils.saveConstrainedRotation(prefs, "background_rotation", rotation)
                            onSave()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun UITemplateOverlay(backgroundTransparency: Float = 0.0f) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 2.dp.toPx()
        // Use transparency value to show how cards will look with background transparency
        // When transparency is 0.0f (solid), show normal preview alpha
        // When transparency is > 0.0f, show how transparent the background will be
        val previewAlpha = if (backgroundTransparency > 0.0f) (1.0f - backgroundTransparency) else 0.3f
        val primaryCardColor = Color.Green.copy(alpha = previewAlpha * 0.8f)
        val secondaryCardColor = Color.White.copy(alpha = previewAlpha * 0.7f)
        val infoCardColor = Color.White.copy(alpha = previewAlpha * 0.6f)
        val outlineColor = Color.White.copy(alpha = 0.8f)
        
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        val cardSpacing = 16.dp.toPx()
        
        // Account for top bar and status bar
        var currentY = padding + 80.dp.toPx()
        
        // 1. Primary Status Card (Working card)
        val primaryCardHeight = 100.dp.toPx()
        drawRoundRect(
            color = primaryCardColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(width - 2 * padding, primaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(width - 2 * padding, primaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
        
        currentY += primaryCardHeight + cardSpacing
        
        // 2. Two Secondary Cards (Superusers and Modules)
        val secondaryCardHeight = 80.dp.toPx()
        val secondaryCardWidth = (width - 2 * padding - cardSpacing) / 2
        
        // Superusers card
        drawRoundRect(
            color = secondaryCardColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(secondaryCardWidth, secondaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(secondaryCardWidth, secondaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
        
        // Modules card
        drawRoundRect(
            color = secondaryCardColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding + secondaryCardWidth + cardSpacing, currentY),
            size = androidx.compose.ui.geometry.Size(secondaryCardWidth, secondaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding + secondaryCardWidth + cardSpacing, currentY),
            size = androidx.compose.ui.geometry.Size(secondaryCardWidth, secondaryCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
        
        currentY += secondaryCardHeight + cardSpacing
        
        // 3. Info Card (System information)
        val infoCardHeight = 200.dp.toPx()
        drawRoundRect(
            color = infoCardColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(width - 2 * padding, infoCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(padding, currentY),
            size = androidx.compose.ui.geometry.Size(width - 2 * padding, infoCardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
        
        // Bottom navigation area
        val bottomY = height - 80.dp.toPx()
        val navHeight = 60.dp.toPx()
        
        drawRoundRect(
            color = secondaryCardColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, bottomY),
            size = androidx.compose.ui.geometry.Size(width, navHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, bottomY),
            size = androidx.compose.ui.geometry.Size(width, navHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f),
            style = Stroke(strokeWidth)
        )
        
        // Add text labels
        val textPaint = android.graphics.Paint().apply {
            color = Color.White.copy(alpha = 0.9f).toArgb()
            textSize = 11.sp.toPx()
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        // Label for primary card
        drawContext.canvas.nativeCanvas.drawText(
            "Working Status",
            padding + 12.dp.toPx(),
            padding + 80.dp.toPx() + 25.dp.toPx(),
            textPaint
        )
        
        // Labels for secondary cards
        drawContext.canvas.nativeCanvas.drawText(
            "Superusers",
            padding + 12.dp.toPx(),
            padding + 80.dp.toPx() + primaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
        
        drawContext.canvas.nativeCanvas.drawText(
            "Modules",
            padding + secondaryCardWidth + cardSpacing + 12.dp.toPx(),
            padding + 80.dp.toPx() + primaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
        
        // Label for info card
        drawContext.canvas.nativeCanvas.drawText(
            "System Info",
            padding + 12.dp.toPx(),
            padding + 80.dp.toPx() + primaryCardHeight + cardSpacing + secondaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
    }
}