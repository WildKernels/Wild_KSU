package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.sqrt

object BlurUtils {
    
    /**
     * Apply Gaussian blur to a bitmap using a custom implementation
     * @param bitmap Source bitmap
     * @param radius Blur radius (0-25 for good performance)
     * @return Blurred bitmap
     */
    fun applyGaussianBlur(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0f) return bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calculate Gaussian kernel
        val kernelRadius = (radius * 2).toInt().coerceAtMost(25)
        val kernel = createGaussianKernel(kernelRadius)
        
        // Horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f
                var weightSum = 0f
                
                for (i in -kernelRadius..kernelRadius) {
                    val sampleX = (x + i).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + sampleX]
                    val weight = kernel[i + kernelRadius]
                    
                    r += ((pixel shr 16) and 0xFF) * weight
                    g += ((pixel shr 8) and 0xFF) * weight
                    b += (pixel and 0xFF) * weight
                    a += ((pixel shr 24) and 0xFF) * weight
                    weightSum += weight
                }
                
                blurredPixels[y * width + x] = (
                    ((a / weightSum).toInt() shl 24) or
                    ((r / weightSum).toInt() shl 16) or
                    ((g / weightSum).toInt() shl 8) or
                    (b / weightSum).toInt()
                )
            }
        }
        
        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f
                var weightSum = 0f
                
                for (i in -kernelRadius..kernelRadius) {
                    val sampleY = (y + i).coerceIn(0, height - 1)
                    val pixel = blurredPixels[sampleY * width + x]
                    val weight = kernel[i + kernelRadius]
                    
                    r += ((pixel shr 16) and 0xFF) * weight
                    g += ((pixel shr 8) and 0xFF) * weight
                    b += (pixel and 0xFF) * weight
                    a += ((pixel shr 24) and 0xFF) * weight
                    weightSum += weight
                }
                
                pixels[y * width + x] = (
                    ((a / weightSum).toInt() shl 24) or
                    ((r / weightSum).toInt() shl 16) or
                    ((g / weightSum).toInt() shl 8) or
                    (b / weightSum).toInt()
                )
            }
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Create a Gaussian kernel for blur calculations
     * @param radius Kernel radius
     * @return Gaussian kernel weights
     */
    private fun createGaussianKernel(radius: Int): FloatArray {
        val size = radius * 2 + 1
        val kernel = FloatArray(size)
        val sigma = radius / 3.0
        val twoSigmaSquare = 2.0 * sigma * sigma
        val sigmaRoot = sqrt(twoSigmaSquare * PI)
        var total = 0.0
        
        for (i in -radius..radius) {
            val distance = i * i
            val index = i + radius
            kernel[index] = (exp(-distance / twoSigmaSquare) / sigmaRoot).toFloat()
            total += kernel[index]
        }
        
        // Normalize kernel
        for (i in kernel.indices) {
            kernel[i] /= total.toFloat()
        }
        
        return kernel
    }
    
    /**
     * Apply fast box blur (alternative to Gaussian for better performance)
     * @param bitmap Source bitmap
     * @param radius Blur radius
     * @return Blurred bitmap
     */
    fun applyBoxBlur(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0f) return bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
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
     * Use RenderEffect for modern Android versions (API 31+)
     * @param radius Blur radius
     * @return RenderEffect for blur
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun createRenderEffectBlur(radius: Float): RenderEffect {
        return RenderEffect.createBlurEffect(
            radius.coerceIn(0.1f, 25f),
            radius.coerceIn(0.1f, 25f),
            Shader.TileMode.CLAMP
        )
    }
    
    /**
     * Determine the best blur method based on device capabilities
     * @param radius Blur radius
     * @return Blur method identifier
     */
    fun getBestBlurMethod(radius: Float): BlurMethod {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 10f -> BlurMethod.RENDER_EFFECT
            radius <= 5f -> BlurMethod.BOX_BLUR
            else -> BlurMethod.GAUSSIAN_BLUR
        }
    }
    
    enum class BlurMethod {
        BOX_BLUR,
        GAUSSIAN_BLUR,
        RENDER_EFFECT
    }
}