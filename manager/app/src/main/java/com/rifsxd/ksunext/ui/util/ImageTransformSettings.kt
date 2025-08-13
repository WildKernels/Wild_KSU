package com.rifsxd.ksunext.ui.util

import android.content.SharedPreferences

data class ImageTransformSettings(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f
) {
    companion object {
        fun loadFromPrefs(prefs: SharedPreferences): ImageTransformSettings {
            return ImageTransformSettings(
                offsetX = prefs.getFloat("background_pos_x", 0f),
                offsetY = prefs.getFloat("background_pos_y", 0f),
                scale = prefs.getFloat("background_scale_x", 1f),
                rotation = prefs.getFloat("background_rotation", 0f),
                brightness = prefs.getFloat("image_brightness", 0f),
                contrast = prefs.getFloat("image_contrast", 1f),
                saturation = prefs.getFloat("image_saturation", 1f),
                hue = prefs.getFloat("image_hue", 0f)
            )
        }
        
        fun saveToPrefs(prefs: SharedPreferences, settings: ImageTransformSettings) {
            prefs.edit()
                .putFloat("background_pos_x", settings.offsetX)
                .putFloat("background_pos_y", settings.offsetY)
                .putFloat("background_scale_x", settings.scale)
                .putFloat("background_rotation", settings.rotation)
                .putFloat("image_brightness", settings.brightness)
                .putFloat("image_contrast", settings.contrast)
                .putFloat("image_saturation", settings.saturation)
                .putFloat("image_hue", settings.hue)
                .apply()
        }
    }
}