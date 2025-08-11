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
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.FlipHorizontal
import androidx.compose.material.icons.filled.FlipVertical
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
    
    // Additional editing states
    var flipHorizontal by remember { mutableStateOf(prefs.getBoolean("background_flip_h", false)) }
    var flipVertical by remember { mutableStateOf(prefs.getBoolean("background_flip_v", false)) }
    var brightness by remember { mutableFloatStateOf(prefs.getFloat("background_brightness", 1.0f)) }
    var contrast by remember { mutableFloatStateOf(prefs.getFloat("background_contrast", 1.0f)) }
    var saturation by remember { mutableFloatStateOf(prefs.getFloat("background_saturation", 1.0f)) }
    
    // UI state
    var showMoreOptions by remember { mutableStateOf(false) }
    
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
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Unified Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Photo Editor",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick rotation button
                    IconButton(onClick = {
                        rotation = (rotation + 90f) % 360f
                    }) {
                        Icon(
                            Icons.Default.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = Color.White
                        )
                    }
                    
                    // Flip horizontal button
                    IconButton(onClick = {
                        flipHorizontal = !flipHorizontal
                    }) {
                        Icon(
                            Icons.Default.FlipHorizontal,
                            contentDescription = "Flip Horizontal",
                            tint = if (flipHorizontal) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    
                    // More options button
                    IconButton(onClick = {
                        showMoreOptions = !showMoreOptions
                    }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .zIndex(1f)
            )
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
                            scaleX = scale * if (flipHorizontal) -1f else 1f,
                            scaleY = scale * if (flipVertical) -1f else 1f,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = rotation,
                            alpha = brightness.coerceIn(0.1f, 2.0f)
                        ),
                    contentScale = ContentScale.Fit, // Match the actual background behavior
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix().apply {
                            // Apply contrast
                            val contrastValue = contrast.coerceIn(0.1f, 3.0f)
                            val translate = (1.0f - contrastValue) / 2.0f * 255.0f
                            val scale = contrastValue
                            this[0, 0] = scale // Red
                            this[1, 1] = scale // Green  
                            this[2, 2] = scale // Blue
                            this[0, 4] = translate // Red offset
                            this[1, 4] = translate // Green offset
                            this[2, 4] = translate // Blue offset
                            
                            // Apply saturation
                            val satValue = saturation.coerceIn(0.0f, 2.0f)
                            val lumR = 0.3086f
                            val lumG = 0.6094f
                            val lumB = 0.0820f
                            val sr = (1 - satValue) * lumR
                            val sg = (1 - satValue) * lumG
                            val sb = (1 - satValue) * lumB
                            
                            this[0, 0] = sr + satValue
                            this[0, 1] = sr
                            this[0, 2] = sr
                            this[1, 0] = sg
                            this[1, 1] = sg + satValue
                            this[1, 2] = sg
                            this[2, 0] = sb
                            this[2, 1] = sb
                            this[2, 2] = sb + satValue
                        }
                    )
                )
                
                // Template overlay showing where UI elements will be
                UITemplateOverlay(backgroundTransparency = backgroundTransparency)
            }
            
            // Expanded options panel
            if (showMoreOptions) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 16.dp)
                        .width(280.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Adjustments",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        
                        // Brightness slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Brightness", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("${(brightness * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
                                valueRange = 0.1f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        
                        // Contrast slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contrast", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("${(contrast * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = contrast,
                                onValueChange = { contrast = it },
                                valueRange = 0.1f..3.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        
                        // Saturation slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saturation", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("${(saturation * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = saturation,
                                onValueChange = { saturation = it },
                                valueRange = 0.0f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.3f))
                        
                        // Additional flip options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { flipVertical = !flipVertical },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (flipVertical) MaterialTheme.colorScheme.primary else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp, 
                                    if (flipVertical) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FlipVertical, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Flip V", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    // Reset all adjustments
                                    brightness = 1.0f
                                    contrast = 1.0f
                                    saturation = 1.0f
                                    flipHorizontal = false
                                    flipVertical = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
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
                    .navigationBarsPadding() // Ensure controls appear above navigation bar
                    .padding(16.dp)
            ) {
                // Fit options row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Auto-fit button - centers and resets transformations
                    OutlinedButton(
                        onClick = {
                            // Auto fit - center and reset all transformations
                            scale = 1.0f // Reset to default scale
                            offsetX = 0.0f
                            offsetY = 0.0f
                            rotation = 0.0f
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fit to Screen")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Zoom to fit button - scales to fill screen while maintaining aspect ratio
                    OutlinedButton(
                        onClick = {
                            // Zoom to fit - scale to fill screen, center, no rotation
                            scale = 1.5f // Scale up to fill more of the screen
                            offsetX = 0.0f
                            offsetY = 0.0f
                            rotation = 0.0f
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zoom to Fit")
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
        
        // The background image now spans the full screen behind system bars
        // Position UI elements as they appear in the main app with full-screen background
        val topAppBarHeight = 64.dp.toPx() // Standard Material3 top app bar height
        val statusBarHeight = 24.dp.toPx() // Approximate status bar height
        
        // Start positioning from the actual content area (after system UI)
        var currentY = padding + topAppBarHeight + statusBarHeight
        
        // 1. Primary Status Card (Working card)
        // Use original card heights since the background spans full screen
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
        
        // Bottom navigation area removed - no longer needed
        
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
            topAppBarHeight + statusBarHeight + padding + 25.dp.toPx(),
            textPaint
        )
        
        // Labels for secondary cards
        drawContext.canvas.nativeCanvas.drawText(
            "Superusers",
            padding + 12.dp.toPx(),
            topAppBarHeight + statusBarHeight + padding + primaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
        
        drawContext.canvas.nativeCanvas.drawText(
            "Modules",
            padding + secondaryCardWidth + cardSpacing + 12.dp.toPx(),
            topAppBarHeight + statusBarHeight + padding + primaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
        
        // Label for info card
        drawContext.canvas.nativeCanvas.drawText(
            "System Info",
            padding + 12.dp.toPx(),
            topAppBarHeight + statusBarHeight + padding + primaryCardHeight + cardSpacing + secondaryCardHeight + cardSpacing + 25.dp.toPx(),
            textPaint
        )
    }
}