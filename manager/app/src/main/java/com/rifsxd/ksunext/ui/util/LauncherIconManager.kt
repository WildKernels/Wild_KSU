package com.rifsxd.ksunext.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.rifsxd.ksunext.R

enum class LauncherIcon(
    val key: String,
    val label: String,
    val previewBackgroundColorResId: Int,
    val aliasClassName: String
) {
    DEFAULT(
        key = "default",
        label = "Default",
        previewBackgroundColorResId = R.color.ic_launcher_background,
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasDefault"
    ),
    MONET(
        key = "monet",
        label = "Monet",
        previewBackgroundColorResId = R.color.launcher_monet_bg,
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasMonet"
    ),
    CUSTOM(
        key = "custom",
        label = "Custom",
        previewBackgroundColorResId = R.color.launcher_custom_bg,
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasCustom"
    ),
    OLD_GREEN(
        key = "old_green",
        label = "Old Green",
        previewBackgroundColorResId = R.color.launcher_old_green_bg,
        aliasClassName = "com.rifsxd.ksunext.ui.MainActivityAliasOldGreen"
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
}
