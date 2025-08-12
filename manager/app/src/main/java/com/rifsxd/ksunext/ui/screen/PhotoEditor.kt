package com.rifsxd.ksunext.ui.screen

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.zIndex
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.core.*
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import coil.compose.rememberAsyncImagePainter
import com.rifsxd.ksunext.ui.util.ImageCropUtils
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
    
    val (minScale, maxScale) = ImageCropUtils.getScaleLimits()
    val (minTranslation, maxTranslation) = ImageCropUtils.getTranslationLimits()
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Unified Top Bar
            UnifiedPhotoEditorTopBar(
                onNavigateBack = onDismiss,
                onRotate = { rotation = (rotation + 90f) % 360f },
                onFlipHorizontal = { flipHorizontal = !flipHorizontal },
                onToggleMoreOptions = { showMoreOptions = !showMoreOptions },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
            )
            
            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp) // Account for unified top bar
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            panZoomLock = false
                        ) { _, pan, zoom, rotationDelta ->
                            // Apply zoom
                            scale = (scale * zoom).coerceIn(minScale, maxScale)
                            // Apply pan/drag
                            offsetX = (offsetX + pan.x).coerceIn(minTranslation, maxTranslation)
                            offsetY = (offsetY + pan.y).coerceIn(minTranslation, maxTranslation)
                            // Apply rotation with finger drag
                            rotation = (rotation + rotationDelta) % 360f
                        }
                    }
            ) {
                // Image preview with full transformations (zoom, drag, and rotation)
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
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix().apply {
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
                
                // Home layout card template overlay - completely non-interactive
                HomeLayoutCardTemplate(
                    modifier = Modifier.fillMaxSize()
                )
                
                // Expanded options panel
                if (showMoreOptions) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
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
                            Icon(Icons.Default.SwapVert, contentDescription = null)
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
                }
                
                // Aligned Bottom Controls
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // First row - Fit options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Auto-fit to screen
                                scale = 1.0f
                                offsetX = 0.0f
                                offsetY = 0.0f
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto Fit")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                // Zoom to fit
                                scale = maxScale * 0.8f
                                offsetX = 0.0f
                                offsetY = 0.0f
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ZoomOutMap, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zoom Fit")
                        }
                    }
                    
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
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                // Save all transformations including rotation and adjustments
                        ImageCropUtils.saveConstrainedScale(prefs, "background_scale_x", scale)
                        ImageCropUtils.saveConstrainedScale(prefs, "background_scale_y", scale)
                        ImageCropUtils.saveConstrainedTranslation(prefs, "background_pos_x", offsetX)
                        ImageCropUtils.saveConstrainedTranslation(prefs, "background_pos_y", offsetY)
                        ImageCropUtils.saveConstrainedRotation(prefs, "background_rotation", rotation)
                        
                        // Save flip states
                        prefs.edit()
                            .putBoolean("background_flip_h", flipHorizontal)
                            .putBoolean("background_flip_v", flipVertical)
                            .putFloat("background_brightness", brightness)
                            .putFloat("background_contrast", contrast)
                            .putFloat("background_saturation", saturation)
                            .apply()
                        
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedPhotoEditorTopBar(
    onNavigateBack: () -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onToggleMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Photo Editor",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Photo Editor",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = onRotate) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                val flipHorizontalPressed = remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        flipHorizontalPressed.value = true
                        onFlipHorizontal()
                    },
                    modifier = Modifier.scale(if (flipHorizontalPressed.value) 0.9f else 1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                LaunchedEffect(flipHorizontalPressed.value) {
                    if (flipHorizontalPressed.value) {
                        delay(100)
                        flipHorizontalPressed.value = false
                    }
                }
                
                IconButton(onClick = onToggleMoreOptions) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun HomeLayoutCardTemplate(
    modifier: Modifier = Modifier
) {
    // Make cards non-interactive to allow touch passthrough to image
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false // Disable scrolling to allow touch passthrough
        ) {
        // Status Card Template (mimicking StatusCard from Home.kt)
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty content as requested - no text
                }
            }
        }

        // Secondary Cards Row Template (mimicking SuperuserCard and ModuleCard)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Secondary Card
                    Box(modifier = Modifier.weight(1f)) {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(100.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Empty content as requested - no text
                            }
                        }
                    }
                    
                    // Second Secondary Card
                    Box(modifier = Modifier.weight(1f)) {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(100.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Empty content as requested - no text
                            }
                        }
                    }
                }
            }
        }

        // Info Card Template (mimicking InfoCard from Home.kt)
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty content as requested - no text
                }
            }
        }
        }
    }
}