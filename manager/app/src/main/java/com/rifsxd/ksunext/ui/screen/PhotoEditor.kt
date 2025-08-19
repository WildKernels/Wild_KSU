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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.with
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
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
import com.rifsxd.ksunext.ui.util.BackgroundCustomization
import com.rifsxd.ksunext.ui.util.BackgroundTransformation
import com.rifsxd.ksunext.ui.util.LocalPhotoEditorSaveCallbackSetter
import com.rifsxd.ksunext.ui.util.LocalPhotoEditorResetCallback
import com.rifsxd.ksunext.ui.util.LocalPhotoEditorScreenRotationCallback
import com.rifsxd.ksunext.ui.util.LocalPhotoEditorScreenRotationLocked

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PhotoEditorScreen(
    imageUri: String,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    
    // Reset background transparency and blur settings to 0% when entering photo editor
    LaunchedEffect(Unit) {
        BackgroundCustomization.resetBackgroundEffects(context)
    }
    
    val saveFunction = { scale: Float, offsetX: Float, offsetY: Float, rotation: Float ->
        // Save transform settings using BackgroundCustomization
        val transformation = BackgroundTransformation(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation
        )
        
        println("PhotoEditor: Saving transformation: $transformation")
        BackgroundCustomization.saveBackgroundSettings(context, imageUri, transformation)
        
        // Also save the background image URI to preferences when user confirms
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString("background_image_uri", imageUri).commit()
        
        println("PhotoEditor: All settings saved, navigating back")
        navigator.popBackStack()
        Unit
    }
    
    // Load existing settings for this specific image or use defaults
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Always start with default settings - no automatic loading of previous settings
    LaunchedEffect(imageUri) {
        // Always reset to defaults when entering photo editor
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        
        println("PhotoEditor: Starting with default transform settings for image: $imageUri")
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
            // Simply navigate back without making any changes to preserve original state
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    var activeMenu by remember { mutableStateOf<String?>(null) }

    var freeFormEditing by remember { mutableStateOf(true) }
    
    var screenRotationLocked by remember { mutableStateOf(false) }
    
    // Get CompositionLocal providers
    val saveCallbackSetter = LocalPhotoEditorSaveCallbackSetter.current
    
    // Set up callbacks for top bar
    LaunchedEffect(Unit) {
        saveCallbackSetter?.invoke {
            onSave()
        }
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
    
    // Define reset function for top bar
    val resetFunction = {
        // Close any open menus
        activeMenu = null
        
        // Reset all settings to defaults (local state only)
        currentScale = 1f
        currentOffsetX = 0f
        currentOffsetY = 0f
        currentRotation = 0f
        freeFormEditing = true
        
        // Reset UI transparency to 0% when reset button is pressed
        BackgroundCustomization.resetUITransparency(context)
        
        // Update transformations (local state only)
        onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
    }
    
    // Define screen rotation toggle function for top bar
    val screenRotationToggleFunction = {
        screenRotationLocked = !screenRotationLocked
    }
    
    CompositionLocalProvider(
        LocalPhotoEditorResetCallback provides resetFunction,
        LocalPhotoEditorScreenRotationCallback provides screenRotationToggleFunction,
        LocalPhotoEditorScreenRotationLocked provides screenRotationLocked
    ) {
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
                .pointerInput(freeFormEditing) {
                    if (freeFormEditing) {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            val newScale = (currentScale * zoom).coerceIn(0.1f, 5f)
                            val newOffsetX = (currentOffsetX + pan.x).coerceIn(-1000f, 1000f)
                            val newOffsetY = (currentOffsetY + pan.y).coerceIn(-1000f, 1000f)
                            val newRotation = if (screenRotationLocked) currentRotation else (currentRotation + rotationChange) % 360f
                            
                            // Update local state
                            currentScale = newScale
                            currentOffsetX = newOffsetX
                            currentOffsetY = newOffsetY
                            currentRotation = newRotation
                            
                            // Notify parent of transform changes
                            onTransformChange(newScale, newOffsetX, newOffsetY, newRotation)
                            
                            // Note: Transform changes are now only saved when user explicitly saves
                            // This allows proper cancellation without persisting temporary changes
                            Unit
                        }
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
        
        // Removed full-screen overlays to keep photo visible
        

        

        
            // Bottom controls overlay - positioned as a separate layer
            Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentWidth()
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
                    .wrapContentWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Single Menu Container with AnimatedContent
                if (activeMenu != "none") {
                    AnimatedContent(
                        targetState = activeMenu,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) with
                            fadeOut(animationSpec = tween(150))
                        }
                    ) { menu ->
                        when (menu) {



                            }
                        }
                    }
                }
                
                // Control Bar with consistent button sizing and precise container fit
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Screen Rotation Toggle - Consistent sizing
                    IconButton(
                        onClick = { screenRotationLocked = !screenRotationLocked },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (screenRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                            contentDescription = "Screen Rotation Toggle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Reset button - Consistent sizing
                    IconButton(
                        onClick = {
                            // Close any open menus
                            activeMenu = null
                            
                            // Reset all settings to defaults (local state only)
                            currentScale = 1f
                            currentOffsetX = 0f
                            currentOffsetY = 0f
                            currentRotation = 0f
                            freeFormEditing = true
                            
                            // Reset UI transparency to 0% when reset button is pressed
                            BackgroundCustomization.resetUITransparency(context)
                            
                            // Update transformations (local state only)
                            onTransformChange(currentScale, currentOffsetX, currentOffsetY, currentRotation)
                            
                            // Note: Reset only affects local state - user must save to persist changes
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
                    
                    // Confirm button - Consistent sizing with primary styling
                    IconButton(
                        onClick = {
                            activeMenu = null
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