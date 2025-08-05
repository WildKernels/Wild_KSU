package com.rifsxd.ksunext.ui.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

data class ImageTransformation(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

fun loadImageBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        Log.e("ImageUtils", "Error loading image bitmap", e)
        null
    }
}

fun applyImageTransformation(
    bitmap: Bitmap,
    transformation: ImageTransformation,
    screenWidth: Int,
    screenHeight: Int
): Bitmap? {
    return try {
        val matrix = Matrix()
        
        // Calculate the scale to fit the screen while maintaining aspect ratio
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val bitmapRatio = bitmapWidth / bitmapHeight
        
        val baseScale = if (bitmapRatio > screenRatio) {
            screenHeight / bitmapHeight
        } else {
            screenWidth / bitmapWidth
        }
        
        val finalScale = baseScale * transformation.scale
        
        // Calculate safe offset bounds
        val scaledWidth = bitmapWidth * finalScale
        val scaledHeight = bitmapHeight * finalScale
        
        val maxOffsetX = max(0f, (scaledWidth - screenWidth) / 2)
        val maxOffsetY = max(0f, (scaledHeight - screenHeight) / 2)
        
        val safeOffsetX = transformation.offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        val safeOffsetY = transformation.offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        
        // Apply transformations
        matrix.postScale(finalScale, finalScale)
        matrix.postTranslate(
            (screenWidth - scaledWidth) / 2 + safeOffsetX,
            (screenHeight - scaledHeight) / 2 + safeOffsetY
        )
        
        // Create the transformed bitmap
        val resultBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(bitmap, matrix, null)
        
        Log.d("ImageUtils", "Applied transformation: scale=$finalScale, offsetX=$safeOffsetX, offsetY=$safeOffsetY")
        resultBitmap
    } catch (e: Exception) {
        Log.e("ImageUtils", "Error applying transformation to bitmap", e)
        null
    }
}

fun Context.saveCustomBackground(
    originalUri: Uri,
    transformation: ImageTransformation
): Uri? {
    return try {
        val originalBitmap = loadImageBitmap(this, originalUri) ?: return null
        
        // Get screen dimensions (you might want to pass these as parameters)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val transformedBitmap = applyImageTransformation(
            originalBitmap,
            transformation,
            screenWidth,
            screenHeight
        ) ?: return null
        
        // Save to internal storage
        val filename = "custom_bg_${System.currentTimeMillis()}.png"
        val file = File(filesDir, filename)
        
        FileOutputStream(file).use { out ->
            transformedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        Log.d("ImageUtils", "Saved custom background to: ${file.absolutePath}")
        Uri.fromFile(file)
    } catch (e: Exception) {
        Log.e("ImageUtils", "Error saving custom background", e)
        null
    }
}