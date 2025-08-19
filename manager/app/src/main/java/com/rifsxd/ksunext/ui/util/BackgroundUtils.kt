package com.rifsxd.ksunext.ui.util

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Data class representing background image transformation parameters
 */
data class BackgroundTransformation(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
)

object BackgroundUtils {
    private const val TAG = "BackgroundUtils"
    private const val TRANSFORMED_BACKGROUND_FILENAME = "custom_background_transformed.jpg"
    
    /**
     * Get bitmap from URI
     * @param context Application context
     * @param uri URI of the image
     * @return Bitmap or null if failed
     */
    fun Context.getImageBitmap(uri: Uri): Bitmap? {
        return try {
            val contentResolver: ContentResolver = contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get image bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Apply transformation to bitmap with screen-aware scaling
     * @param context Application context
     * @param bitmap Source bitmap
     * @param transformation Transformation parameters
     * @return Transformed bitmap
     */
    fun Context.applyTransformationToBitmap(bitmap: Bitmap, transformation: BackgroundTransformation): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Create bitmap with screen proportions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenRatio = screenHeight.toFloat() / screenWidth.toFloat()
        
        // Calculate target dimensions
        val targetWidth: Int
        val targetHeight: Int
        if (width.toFloat() / height.toFloat() > screenRatio) {
            targetHeight = height
            targetWidth = (height / screenRatio).toInt()
        } else {
            targetWidth = width
            targetHeight = (width * screenRatio).toInt()
        }
        
        // Create target bitmap
        val scaledBitmap = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(scaledBitmap)
        
        val matrix = Matrix()
        
        // Ensure valid scale value
        val safeScale = maxOf(0.1f, transformation.scale)
        matrix.postScale(safeScale, safeScale)
        
        // Calculate offset bounds to prevent negative max values
        val widthDiff = (bitmap.width * safeScale - targetWidth)
        val heightDiff = (bitmap.height * safeScale - targetHeight)
        
        // Safe offset calculation
        val maxOffsetX = maxOf(0f, widthDiff / 2)
        val maxOffsetY = maxOf(0f, heightDiff / 2)
        
        // Constrain offsets within bounds
        val safeOffsetX = if (maxOffsetX > 0)
            transformation.offsetX.coerceIn(-maxOffsetX, maxOffsetX) else 0f
        val safeOffsetY = if (maxOffsetY > 0)
            transformation.offsetY.coerceIn(-maxOffsetY, maxOffsetY) else 0f
        
        // Apply translation to matrix
        val translationX = -widthDiff / 2 + safeOffsetX
        val translationY = -heightDiff / 2 + safeOffsetY
        
        matrix.postTranslate(translationX, translationY)
        
        // Apply rotation if specified
        if (transformation.rotation != 0f) {
            matrix.postRotate(transformation.rotation, targetWidth / 2f, targetHeight / 2f)
        }
        
        // Draw transformed bitmap
        canvas.drawBitmap(bitmap, matrix, null)
        
        return scaledBitmap
    }
    
    /**
     * Save transformed background image to internal storage
     * @param context Application context
     * @param uri Source image URI
     * @param transformation Transformation parameters
     * @return URI of saved transformed image or null if failed
     */
    fun Context.saveTransformedBackground(uri: Uri, transformation: BackgroundTransformation): Uri? {
        return try {
            val bitmap = getImageBitmap(uri) ?: return null
            val transformedBitmap = applyTransformationToBitmap(bitmap, transformation)
            
            // Create images directory if it doesn't exist
            val imagesDir = File(filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val file = File(imagesDir, TRANSFORMED_BACKGROUND_FILENAME)
            val outputStream = FileOutputStream(file)
            
            transformedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Successfully saved transformed background to: ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save transformed image: ${e.message}", e)
            null
        }
    }
    
    /**
     * Save background settings to SharedPreferences
     * @param context Application context
     * @param imageUri Original image URI
     * @param transformation Transformation parameters
     */
    fun saveBackgroundSettings(context: Context, imageUri: String, transformation: BackgroundTransformation) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        prefs.edit()
            .putString("background_image_uri", imageUri)
            .putFloat("background_scale_x", transformation.scale)
            .putFloat("background_pos_x", transformation.offsetX)
            .putFloat("background_pos_y", transformation.offsetY)
            .putFloat("background_rotation", transformation.rotation)
            .putFloat("background_transparency", 0.0f)
            .putFloat("background_blur", 0.0f)
            .putString("background_fit_mode", "fit")
            .apply()
            
        Log.d(TAG, "Background settings saved: $transformation")
    }
    
    /**
     * Load background transformation from SharedPreferences
     * @param context Application context
     * @return BackgroundTransformation with saved values or defaults
     */
    fun loadBackgroundTransformation(context: Context): BackgroundTransformation {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        return BackgroundTransformation(
            scale = prefs.getFloat("background_scale_x", 1f),
            offsetX = prefs.getFloat("background_pos_x", 0f),
            offsetY = prefs.getFloat("background_pos_y", 0f),
            rotation = prefs.getFloat("background_rotation", 0f)
        )
    }
    
    /**
     * Reset background transparency and blur settings
     * @param context Application context
     */
    fun resetBackgroundEffects(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("background_transparency", 0.0f)
            .putFloat("background_blur", 0.0f)
            .apply()
            
        Log.d(TAG, "Background effects reset to 0%")
    }
    
    /**
     * Reset UI transparency setting
     * @param context Application context
     */
    fun resetUITransparency(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("ui_transparency", 0.0f)
            .apply()
            
        Log.d(TAG, "UI transparency reset to 0%")
    }
}