package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

val LocalPhotoEditorSave = compositionLocalOf<((Float, Float, Float, Float) -> Unit)?> { null }
val LocalPhotoEditorSaveCallback = compositionLocalOf<(() -> Unit)?> { null }

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
        prefs.edit()
            .putString("background_image_uri", imageUri)
            .putFloat("background_scale_x", scale)
            .putFloat("background_pos_x", offsetX)
            .putFloat("background_pos_y", offsetY)
            .putFloat("background_rotation", rotation)
            .putFloat("background_transparency", currentTransparency)
            .putString("background_fit_mode", "fit")
            .apply()
        navigator.popBackStack()
        Unit
    }
    
    CompositionLocalProvider(
        LocalPhotoEditorSave provides saveFunction,
        LocalPhotoEditorSaveCallback provides {
            // This will be overridden by PhotoEditor with current transform values
        }
    ) {
        PhotoEditor(
            imageUri = Uri.parse(imageUri)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val saveFunction = LocalPhotoEditorSave.current
    
    // Load saved transform states or use defaults
    var scale by remember { mutableFloatStateOf(prefs.getFloat("background_scale_x", 1f)) }
    var offsetX by remember { mutableFloatStateOf(prefs.getFloat("background_pos_x", 0f)) }
    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("background_pos_y", 0f)) }
    var rotation by remember { mutableFloatStateOf(prefs.getFloat("background_rotation", 0f)) }
    
    // Provide save callback with current transform values
    CompositionLocalProvider(
        LocalPhotoEditorSaveCallback provides {
            saveFunction?.invoke(scale, offsetX, offsetY, rotation)
        }
    ) {
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
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offsetX = (offsetX + pan.x).coerceIn(-1000f, 1000f)
                        offsetY = (offsetY + pan.y).coerceIn(-1000f, 1000f)
                        rotation = (rotation + rotationChange) % 360f
                        
                        // Save to preferences
                        prefs.edit()
                            .putFloat("background_scale_x", scale)
                            .putFloat("background_pos_x", offsetX)
                            .putFloat("background_pos_y", offsetY)
                            .putFloat("background_rotation", rotation)
                            .apply()
                        Unit
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotation,
                    transformOrigin = TransformOrigin.Center
                ),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
        }
    }
}