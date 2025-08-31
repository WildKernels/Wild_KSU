package com.twj.wksu.ui.util

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri

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

object BackgroundCustomization {

    private const val TRANSFORMED_BACKGROUND_FILENAME = "custom_background_transformed.jpg"
    private const val BACKGROUND_IMAGE_FILENAME = "background_image.jpg"
    private const val TEMP_BACKGROUND_PREFIX = "temp_background_"
    
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
            
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save background settings to SharedPreferences
     * @param context Application context
     * @param imageUri Original image URI
     * @param transformation Transformation parameters
     */
    fun saveBackgroundSettings(context: Context, imageUri: String, transformation: BackgroundTransformation, saveUri: Boolean = false) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        val editor = prefs.edit()
            .putFloat("background_scale_x", transformation.scale)
            .putFloat("background_pos_x", transformation.offsetX)
            .putFloat("background_pos_y", transformation.offsetY)
            .putFloat("background_rotation", transformation.rotation)
            .putString("background_fit_mode", "fit")
            
        // Only save URI if explicitly requested
        if (saveUri) {
            editor.putString("background_image_uri", imageUri)
        }
        
        editor.apply()
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
    }
    
    /**
     * Apply simple blur effect to bitmap
     * @param bitmap Source bitmap
     * @param radius Blur radius (0-25)
     * @return Blurred bitmap
     */
    fun applyBlur(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0f) {
            return bitmap
        }
        
        // Convert HARDWARE bitmap to software bitmap if needed
        val workingBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        
        val width = workingBitmap.width
        val height = workingBitmap.height
        val pixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        
        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val r = radius.toInt().coerceAtMost(25)
        
        // Horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var red = 0
                var green = 0
                var blue = 0
                var alpha = 0
                var count = 0
                
                for (i in -r..r) {
                    val sampleX = (x + i).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + sampleX]
                    
                    red += (pixel shr 16) and 0xFF
                    green += (pixel shr 8) and 0xFF
                    blue += pixel and 0xFF
                    alpha += (pixel shr 24) and 0xFF
                    count++
                }
                
                blurredPixels[y * width + x] = (
                    ((alpha / count) shl 24) or
                    ((red / count) shl 16) or
                    ((green / count) shl 8) or
                    (blue / count)
                )
            }
        }
        
        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var red = 0
                var green = 0
                var blue = 0
                var alpha = 0
                var count = 0
                
                for (i in -r..r) {
                    val sampleY = (y + i).coerceIn(0, height - 1)
                    val pixel = blurredPixels[sampleY * width + x]
                    
                    red += (pixel shr 16) and 0xFF
                    green += (pixel shr 8) and 0xFF
                    blue += pixel and 0xFF
                    alpha += (pixel shr 24) and 0xFF
                    count++
                }
                
                pixels[y * width + x] = (
                    ((alpha / count) shl 24) or
                    ((red / count) shl 16) or
                    ((green / count) shl 8) or
                    (blue / count)
                )
            }
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Copy an image from URI to internal storage
     * @param context Application context
     * @param sourceUri URI of the image to copy
     * @return File path of the copied image, or null if failed
     */
    /**
     * Copy image to internal storage for temporary editing
     * Uses unique filename to avoid overwriting existing background
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                return null
            }
            
            // Create internal storage directory for images
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            // Create a unique temporary filename to avoid overwriting existing background
            val timestamp = System.currentTimeMillis()
            val tempFileName = "${TEMP_BACKGROUND_PREFIX}${timestamp}.jpg"
            val destinationFile = File(imagesDir, tempFileName)
            
            // Copy the image
            val outputStream = FileOutputStream(destinationFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            return destinationFile.absolutePath
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Copy temporary image to permanent background location
     * Used when user confirms saving the background
     */
    fun copyTempImageToPermanent(context: Context, tempImagePath: String): String? {
        return try {
            val tempFile = File(tempImagePath)
            if (!tempFile.exists()) {
                return null
            }
            
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val permanentFile = File(imagesDir, BACKGROUND_IMAGE_FILENAME)
            
            // Copy temp file to permanent location
            tempFile.copyTo(permanentFile, overwrite = true)
            
            // Clean up temp file
            tempFile.delete()
            
            return permanentFile.absolutePath
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get the internal storage path for background image
     * @param context Application context
     * @return File path if exists, null otherwise
     */
    fun getInternalBackgroundImagePath(context: Context): String? {
        val imagesDir = File(context.filesDir, "images")
        val backgroundFile = File(imagesDir, BACKGROUND_IMAGE_FILENAME)
        
        return if (backgroundFile.exists()) {
            backgroundFile.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Delete the internal background image
     * @param context Application context
     * @return true if deleted successfully, false otherwise
     */
    fun deleteInternalBackgroundImage(context: Context): Boolean {
        return try {
            val imagesDir = File(context.filesDir, "images")
            val backgroundFile = File(imagesDir, BACKGROUND_IMAGE_FILENAME)
            
            if (backgroundFile.exists()) {
                val deleted = backgroundFile.delete()
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Convert file path to file:// URI string
     * @param filePath Absolute file path
     * @return file:// URI string
     */
    fun filePathToUri(filePath: String): String {
        return "file://$filePath"
    }
}
