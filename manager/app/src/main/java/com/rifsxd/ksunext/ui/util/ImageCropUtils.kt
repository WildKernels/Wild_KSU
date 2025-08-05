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
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.rifsxd.ksunext.ui.component.ImageCropSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object ImageCropUtils {
    // Constants for transformation limits (inspired by SukiSU Ultra's security limits)
    private const val MIN_SCALE = 0.5f
    private const val MAX_SCALE = 3.0f
    private const val MAX_TRANSLATION = 500f
    private const val TAG = "ImageCropUtils"
    
    fun saveImageCropSettings(prefs: SharedPreferences, uri: String, settings: ImageCropSettings) {
        prefs.edit().apply {
            putString("background_image_uri", uri)
            putFloat("background_scale_x", settings.scale)
            putFloat("background_pos_x", settings.offsetX)
            putFloat("background_pos_y", settings.offsetY)
            putFloat("background_rotation", settings.rotation)
            apply()
        }
        Log.d(TAG, "Saved crop settings: scale=${settings.scale}, offsetX=${settings.offsetX}, offsetY=${settings.offsetY}, rotation=${settings.rotation}")
    }
    
    fun loadImageCropSettings(prefs: SharedPreferences): ImageCropSettings {
        return ImageCropSettings(
            scale = prefs.getFloat("background_scale_x", 1f),
            offsetX = prefs.getFloat("background_pos_x", 0f),
            offsetY = prefs.getFloat("background_pos_y", 0f),
            rotation = prefs.getFloat("background_rotation", 0f)
        )
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
    
    fun applyCropTransformation(
        bitmap: Bitmap,
        settings: ImageCropSettings,
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
            
            Log.d(TAG, "Applied crop transformation: scale=${settings.scale}, offsetX=${settings.offsetX}, offsetY=${settings.offsetY}, rotation=${settings.rotation}")
            return croppedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error applying crop transformation", e)
            return bitmap
        }
    }
    
    // Simplified transformation for crop settings only
    fun getSimpleCropTransformation(
        prefs: SharedPreferences
    ): androidx.compose.ui.Modifier.() -> androidx.compose.ui.Modifier {
        return {
            val cropSettings = loadImageCropSettings(prefs)
            graphicsLayer(
                scaleX = constrainScale(cropSettings.scale),
                scaleY = constrainScale(cropSettings.scale),
                translationX = constrainTranslation(cropSettings.offsetX),
                translationY = constrainTranslation(cropSettings.offsetY),
                rotationZ = constrainRotation(cropSettings.rotation)
            )
        }
    }
    
    // Security functions to ensure transformation limits (inspired by SukiSU Ultra)
    private fun constrainScale(scale: Float): Float {
        return max(MIN_SCALE, min(MAX_SCALE, scale))
    }
    
    private fun constrainTranslation(translation: Float): Float {
        return max(-MAX_TRANSLATION, min(MAX_TRANSLATION, translation))
    }
    
    private fun constrainRotation(rotation: Float): Float {
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
}