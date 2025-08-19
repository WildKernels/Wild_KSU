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
import com.rifsxd.ksunext.ui.util.Background
import com.rifsxd.ksunext.ui.util.BackgroundTransformation
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
        Background.resetBackgroundEffects(context)
    }
    
    val saveFunction = { scale: Float, offsetX: Float, offsetY: Float, rotation: Float ->
        // Save transform settings using Background
        val transformation = BackgroundTransformation(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation
        )
        
        println("PhotoEditor: Saving transformation: $transformation")
        Background.saveBackgroundSettings(context, imageUri, transformation)
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
    
    // Get callbacks from CompositionLocal
    val resetCallback = LocalPhotoEditorResetCallback.current
    val screenRotationCallback = LocalPhotoEditorScreenRotationCallback.current
    val screenRotationLocked = LocalPhotoEditorScreenRotationLocked.current

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
        },
        screenRotationLocked = screenRotationLocked.value,
        onReset = {
            // Reset transform values to defaults
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            rotation = 0f
            resetCallback()
        },
        onScreenRotationToggle = {
            screenRotationCallback()
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
    onCancel: () -> Unit = {},
    screenRotationLocked: Boolean = false,
    onReset: () -> Unit = {},
    onScreenRotationToggle: () -> Unit = {}
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
    
    // screenRotationLocked is now passed as a parameter from the top bar
    
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
        
        // Bottom navigation buttons have been moved to the top bar
    }