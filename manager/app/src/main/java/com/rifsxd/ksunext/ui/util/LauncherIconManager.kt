package com.rifsxd.ksunext.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

enum class LauncherIcon(
    val key: String,
    val label: String,
    val aliasClassName: String
) {
    DEFAULT(
        key = "default",
        label = "Default",
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasDefault"
    ),
    MONET(
        key = "monet",
        label = "Monet",
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasMonet"
    );

    companion object {
        fun fromKey(key: String?): LauncherIcon {
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }
    }
}

object LauncherIconManager {
    const val PREF_KEY = "launcher_icon"
    private const val PREFS_NAME = "settings"

    fun getSelected(context: Context): LauncherIcon {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LauncherIcon.fromKey(prefs.getString(PREF_KEY, null))
    }

    fun setSelected(context: Context, icon: LauncherIcon) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, icon.key).apply()
        apply(context, icon)
    }

    fun applySaved(context: Context) {
        apply(context, getSelected(context))
    }

    fun loadPreviewBitmap(context: Context, icon: LauncherIcon, sizePx: Int): Bitmap? {
        return runCatching {
            val pm = context.packageManager
            val component = ComponentName(context.packageName, icon.aliasClassName)
            val drawable = runCatching { pm.getActivityIcon(component) }.getOrElse {
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.getActivityInfo(
                        component,
                        PackageManager.ComponentInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
                    ).loadIcon(pm)
                } else {
                    @Suppress("DEPRECATION")
                    pm.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS).loadIcon(pm)
                }
            }
            drawableToBitmap(drawable, sizePx)
        }.getOrNull()
    }

    private fun apply(context: Context, icon: LauncherIcon) {
        val pm = context.packageManager
        val packageName = context.packageName

        LauncherIcon.entries.forEach { entry ->
            val shouldEnable = entry == icon
            val state = if (shouldEnable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            pm.setComponentEnabledSetting(
                ComponentName(packageName, entry.aliasClassName),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val targetSize = sizePx.coerceAtLeast(1)

        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap.width == targetSize && bitmap.height == targetSize) return bitmap
        }

        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)

        return bitmap
    }
}
