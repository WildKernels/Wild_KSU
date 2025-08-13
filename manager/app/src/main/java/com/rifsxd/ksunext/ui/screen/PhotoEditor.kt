package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
        prefs.edit()
            .putString("background_image_uri", imageUri)
            .putFloat("image_scale", scale)
            .putFloat("image_offset_x", offsetX)
            .putFloat("image_offset_y", offsetY)
            .putFloat("image_rotation", rotation)
            .putFloat("background_transparency", 1.0f)
            .putString("background_fit_mode", "fit")
            .apply()
        navigator.popBackStack()
    }
    
    CompositionLocalProvider(
        LocalPhotoEditorSave provides saveFunction
    ) {
        PhotoEditor(
            imageUri = Uri.parse(imageUri),
            onDismiss = { navigator.popBackStack() },
            onSave = { scale, offsetX, offsetY, rotation -> saveFunction(scale, offsetX, offsetY, rotation) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float) -> Unit // scale, offsetX, offsetY, rotation
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Simple transform states
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
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
        
        // Simple checkmark button
        FloatingActionButton(
            onClick = {
                onSave(scale, offsetX, offsetY, rotation)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}