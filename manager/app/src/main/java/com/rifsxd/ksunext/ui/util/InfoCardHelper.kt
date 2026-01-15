package com.rifsxd.ksunext.ui.util

import android.content.Context
import com.rifsxd.ksunext.R
import org.json.JSONArray
import org.json.JSONObject

data class InfoCardConfigItem(
    val id: String,
    val visible: Boolean
)

object InfoCardHelper {
    const val PREF_KEY = "info_card_config"

    val ALL_ITEMS = listOf(
        "manager_version",
        "hook_mode",
        "mount_system",
        "susfs_version",
        "bbg_version",
        "zygisk_status",
        "kernel_version",
        "android_version",
        "abis",
        "selinux_status"
    )

    fun getLabelResId(id: String): Int {
        return when (id) {
            "manager_version" -> R.string.home_manager_version
            "hook_mode" -> R.string.hook_mode
            "mount_system" -> R.string.home_mount_system
            "susfs_version" -> R.string.home_susfs_version
            "bbg_version" -> R.string.home_bbg_version
            "zygisk_status" -> R.string.zygisk_status
            "kernel_version" -> R.string.home_kernel
            "android_version" -> R.string.home_android
            "abis" -> R.string.home_abi
            "selinux_status" -> R.string.home_selinux_status
            else -> R.string.app_name // Fallback
        }
    }

    fun getConfig(context: Context): List<InfoCardConfigItem> {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val jsonString = prefs.getString(PREF_KEY, null)
        
        if (jsonString == null) {
            return ALL_ITEMS.map { InfoCardConfigItem(it, true) }
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val items = mutableListOf<InfoCardConfigItem>()
            val seenIds = mutableSetOf<String>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val visible = obj.getBoolean("visible")
                if (id in ALL_ITEMS) {
                    items.add(InfoCardConfigItem(id, visible))
                    seenIds.add(id)
                }
            }
            
            // Add any missing new items at the end
            ALL_ITEMS.forEach { id ->
                if (id !in seenIds) {
                    items.add(InfoCardConfigItem(id, true))
                }
            }
            
            return items
        } catch (e: Exception) {
            return ALL_ITEMS.map { InfoCardConfigItem(it, true) }
        }
    }

    fun saveConfig(context: Context, items: List<InfoCardConfigItem>) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("visible", item.visible)
            jsonArray.put(obj)
        }
        prefs.edit().putString(PREF_KEY, jsonArray.toString()).apply()
    }
}
