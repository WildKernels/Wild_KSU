package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

// ImageTransformSettings is now defined in its own file

object BackgroundEditorUtils {
    // Constants for transformation limits (matching PhotoEditor ranges)
    private const val MIN_SCALE = 0.1f
    private const val MAX_SCALE = 5.0f
    private const val MAX_TRANSLATION = 1000f
    private const val TAG = "BackgroundEditorUtils"
    
    fun saveImageTransformSettings(prefs: SharedPreferences, uri: String, settings: ImageTransformSettings) {
        prefs.edit().apply {
            // Don't save URI here - it's already saved by PhotoEditor
            putFloat("background_scale_x", settings.scale)
            putFloat("background_pos_x", settings.offsetX)
            putFloat("background_pos_y", settings.offsetY)
            putFloat("background_rotation", settings.rotation)
            // Save image adjustment settings as well
            putFloat("image_brightness", settings.brightness)
            putFloat("image_contrast", settings.contrast)
            putFloat("image_saturation", settings.saturation)
            putFloat("image_hue", settings.hue)
            apply()
        }
        Log.d(TAG, "Saved transform settings for URI $uri: scale=${settings.scale}, offsetX=${settings.offsetX}, offsetY=${settings.offsetY}, rotation=${settings.rotation}, brightness=${settings.brightness}, contrast=${settings.contrast}, saturation=${settings.saturation}, hue=${settings.hue}")
    }
    
    fun loadImageTransformSettings(prefs: SharedPreferences): ImageTransformSettings {
        return ImageTransformSettings(
            scale = prefs.getFloat("background_scale_x", 1f),
            offsetX = prefs.getFloat("background_pos_x", 0f),
            offsetY = prefs.getFloat("background_pos_y", 0f),
            rotation = prefs.getFloat("background_rotation", 0f),
            brightness = prefs.getFloat("image_brightness", 0f),
            contrast = prefs.getFloat("image_contrast", 1f),
            saturation = prefs.getFloat("image_saturation", 1f),
            hue = prefs.getFloat("image_hue", 0f)
        )
    }
    
    fun clearImageTransformSettings(prefs: SharedPreferences) {
        prefs.edit().apply {
            remove("background_scale_x")
            remove("background_pos_x")
            remove("background_pos_y")
            remove("background_rotation")
            remove("image_brightness")
            remove("image_contrast")
            remove("image_saturation")
            remove("image_hue")
            apply()
        }
        Log.d(TAG, "Cleared image transform settings")
    }
    
    suspend fun loadImageBitmap(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .build()
                
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    result.drawable.let { drawable ->
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    }
                } else {
                    Log.e(TAG, "Failed to load image from URI: $uri")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image bitmap", e)
                null
            }
        }
    }
    
    fun applyImageTransformation(
        bitmap: Bitmap,
        settings: ImageTransformSettings,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        try {
            val matrix = Matrix().apply {
                // Apply transformations in the correct order: scale, rotate, translate
                postScale(settings.scale, settings.scale)
                postRotate(constrainRotation(settings.rotation))
                postTranslate(settings.offsetX, settings.offsetY)
            }
            
            val croppedBitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(croppedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bitmap, matrix, paint)
            
            Log.d(TAG, "Applied image transformation: scale=${settings.scale}, offsetX=${settings.offsetX}, offsetY=${settings.offsetY}, rotation=${settings.rotation}")
            return croppedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error applying image transformation", e)
            return bitmap
        }
    }
    
    // Simplified transformation for image settings only
    // Note: All transformations are now applied via graphicsLayer
    fun getSimpleImageTransformation(
        prefs: SharedPreferences
    ): androidx.compose.ui.Modifier.() -> androidx.compose.ui.Modifier {
        return {
            val transformSettings = loadImageTransformSettings(prefs)
            // Apply transformations directly since PhotoEditor now uses consistent sensitivity
            // No need to scale translation values as gesture handling is now zoom-independent
            val scale = constrainScale(transformSettings.scale)
            graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = constrainTranslation(transformSettings.offsetX),
                translationY = constrainTranslation(transformSettings.offsetY),
                rotationZ = constrainRotation(transformSettings.rotation),
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            )
        }
    }
    
    // Security functions to ensure transformation limits (inspired by SukiSU Ultra)
    fun constrainScale(scale: Float): Float {
        return max(MIN_SCALE, min(MAX_SCALE, scale))
    }
    
    fun constrainTranslation(translation: Float): Float {
        return max(-MAX_TRANSLATION, min(MAX_TRANSLATION, translation))
    }
    
    fun constrainRotation(rotation: Float): Float {
        return rotation % 360f
    }
    
    // Helper functions for saving constrained values
    fun saveConstrainedScale(prefs: SharedPreferences, key: String, value: Float) {
        prefs.edit().putFloat(key, constrainScale(value)).apply()
    }
    
    fun saveConstrainedTranslation(prefs: SharedPreferences, key: String, value: Float) {
        prefs.edit().putFloat(key, constrainTranslation(value)).apply()
    }
    
    fun saveConstrainedRotation(prefs: SharedPreferences, key: String, value: Float) {
        prefs.edit().putFloat(key, constrainRotation(value)).apply()
    }
    
    // Get transformation limits for UI
    fun getScaleLimits(): Pair<Float, Float> = Pair(MIN_SCALE, MAX_SCALE)
    fun getTranslationLimits(): Pair<Float, Float> = Pair(-MAX_TRANSLATION, MAX_TRANSLATION)
    
    // Get image transformation based on fit mode and transform settings
    fun getImageTransformation(
        prefs: SharedPreferences,
        fitMode: String
    ): androidx.compose.ui.Modifier.() -> androidx.compose.ui.Modifier {
        return when (fitMode) {
            "custom_crop", "position_adjust" -> getSimpleImageTransformation(prefs)
            else -> { { this } } // No transformation for fit mode to prevent zoom issues
        }
    }
}