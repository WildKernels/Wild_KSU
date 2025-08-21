package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CustomizationManager handles all user customization preferences and settings
 * This includes background customization, icon themes, UI preferences, and other user settings
 */
object CustomizationManager {
    
    // SharedPreferences keys for customization settings
    object Keys {
        const val SELECTED_ICON_TYPE = "selected_icon_type"
        const val INFO_CARD_ALWAYS_EXPANDED = "info_card_always_expanded"
        const val SHOW_HELP_CARD = "show_help_card"
        const val INFO_CARD_ITEM_ORDER = "info_card_item_order"
        
        // Info card visibility settings
        const val SHOW_MANAGER_VERSION = "info_card_show_manager_version"
        const val SHOW_HOOK_MODE = "info_card_show_hook_mode"
        const val SHOW_MOUNT_SYSTEM = "info_card_show_mount_system"
        const val SHOW_SUSFS_STATUS = "info_card_show_susfs_status"
        const val SHOW_ZYGISK_STATUS = "info_card_show_zygisk_status"
        const val SHOW_KERNEL_VERSION = "info_card_show_kernel_version"
        const val SHOW_ANDROID_VERSION = "info_card_show_android_version"
        const val SHOW_ABI = "info_card_show_abi"
        const val SHOW_SELINUX_STATUS = "info_card_show_selinux_status"
        
        // Module settings
        const val KEEP_MODULE_CARDS_EXPANDED = "keep_module_cards_expanded"
        const val ENABLE_MODULE_BANNERS = "enable_module_banners"
        const val HIDE_MODULE_DETAILS = "hide_module_details"
        
        // Superuser settings
        const val INDIVIDUAL_APP_CARDS = "individual_app_cards"
        const val HIDE_FAVORITES_AUTOMATICALLY = "hide_favorites_automatically"
        const val DISABLE_FAVORITE_BUTTON = "disable_favorite_button"
    }
    
    // Default values for settings
    object Defaults {
        const val SELECTED_ICON_TYPE = "SEASONAL"
        const val INFO_CARD_ALWAYS_EXPANDED = false
        const val SHOW_HELP_CARD = true
        const val SHOW_MANAGER_VERSION = true
        const val SHOW_HOOK_MODE = true
        const val SHOW_MOUNT_SYSTEM = true
        const val SHOW_SUSFS_STATUS = true
        const val SHOW_ZYGISK_STATUS = true
        const val SHOW_KERNEL_VERSION = true
        const val SHOW_ANDROID_VERSION = true
        const val SHOW_ABI = true
        const val SHOW_SELINUX_STATUS = true
        const val KEEP_MODULE_CARDS_EXPANDED = false
        const val ENABLE_MODULE_BANNERS = true
        const val HIDE_MODULE_DETAILS = false
        const val INDIVIDUAL_APP_CARDS = false
        const val HIDE_FAVORITES_AUTOMATICALLY = false
        const val DISABLE_FAVORITE_BUTTON = true
        
        val INFO_CARD_DEFAULT_ORDER = listOf(
            "info_card_show_manager_version",
            "info_card_show_hook_mode", 
            "info_card_show_mount_system",
            "info_card_show_susfs_status",
            "info_card_show_zygisk_status",
            "info_card_show_kernel_version",
            "info_card_show_android_version",
            "info_card_show_abi",
            "info_card_show_selinux_status"
        )
    }
    
    // State flows for reactive UI updates
    private val _iconTypeFlow = MutableStateFlow(Defaults.SELECTED_ICON_TYPE)
    val iconTypeFlow: StateFlow<String> = _iconTypeFlow.asStateFlow()
    
    private val _infoCardExpandedFlow = MutableStateFlow(Defaults.INFO_CARD_ALWAYS_EXPANDED)
    val infoCardExpandedFlow: StateFlow<Boolean> = _infoCardExpandedFlow.asStateFlow()
    
    private val _showHelpCardFlow = MutableStateFlow(Defaults.SHOW_HELP_CARD)
    val showHelpCardFlow: StateFlow<Boolean> = _showHelpCardFlow.asStateFlow()
    
    /**
     * Get SharedPreferences for customization settings
     */
    fun getCustomizationPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    
    /**
     * Initialize CustomizationManager with current preferences
     */
    fun initialize(context: Context) {
        val prefs = getCustomizationPrefs(context)
        _iconTypeFlow.value = prefs.getString(Keys.SELECTED_ICON_TYPE, Defaults.SELECTED_ICON_TYPE) ?: Defaults.SELECTED_ICON_TYPE
        _infoCardExpandedFlow.value = prefs.getBoolean(Keys.INFO_CARD_ALWAYS_EXPANDED, Defaults.INFO_CARD_ALWAYS_EXPANDED)
        _showHelpCardFlow.value = prefs.getBoolean(Keys.SHOW_HELP_CARD, Defaults.SHOW_HELP_CARD)
    }
    
    /**
     * Update icon type preference
     */
    fun updateIconType(context: Context, iconType: String) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putString(Keys.SELECTED_ICON_TYPE, iconType).apply()
        _iconTypeFlow.value = iconType
    }
    
    /**
     * Update info card expanded preference
     */
    fun updateInfoCardExpanded(context: Context, expanded: Boolean) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putBoolean(Keys.INFO_CARD_ALWAYS_EXPANDED, expanded).apply()
        _infoCardExpandedFlow.value = expanded
    }
    
    /**
     * Update help card visibility preference
     */
    fun updateShowHelpCard(context: Context, show: Boolean) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putBoolean(Keys.SHOW_HELP_CARD, show).apply()
        _showHelpCardFlow.value = show
    }
    
    /**
     * Get info card item order
     */
    fun getInfoCardOrder(context: Context): List<String> {
        val prefs = getCustomizationPrefs(context)
        val savedOrder = prefs.getString(Keys.INFO_CARD_ITEM_ORDER, null)
        
        return if (savedOrder.isNullOrEmpty()) {
            Defaults.INFO_CARD_DEFAULT_ORDER
        } else {
            val saved = savedOrder.split(",")
            val result = saved.filter { key -> Defaults.INFO_CARD_DEFAULT_ORDER.contains(key) }.toMutableList()
            Defaults.INFO_CARD_DEFAULT_ORDER.forEach { key ->
                if (!result.contains(key)) result.add(key)
            }
            result
        }
    }
    
    /**
     * Update info card item order
     */
    fun updateInfoCardOrder(context: Context, order: List<String>) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putString(Keys.INFO_CARD_ITEM_ORDER, order.joinToString(",")).apply()
    }
    
    /**
     * Get boolean preference with default value
     */
    fun getBooleanPreference(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        val prefs = getCustomizationPrefs(context)
        return prefs.getBoolean(key, defaultValue)
    }
    
    /**
     * Update boolean preference
     */
    fun updateBooleanPreference(context: Context, key: String, value: Boolean) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putBoolean(key, value).apply()
    }
    
    /**
     * Get string preference with default value
     */
    fun getStringPreference(context: Context, key: String, defaultValue: String? = null): String? {
        val prefs = getCustomizationPrefs(context)
        return prefs.getString(key, defaultValue)
    }
    
    /**
     * Update string preference
     */
    fun updateStringPreference(context: Context, key: String, value: String?) {
        val prefs = getCustomizationPrefs(context)
        prefs.edit().putString(key, value).apply()
    }
    
    /**
     * Reset all customization preferences to defaults
     */
    fun resetToDefaults(context: Context) {
        val prefs = getCustomizationPrefs(context)
        val editor = prefs.edit()
        
        // Reset all customization preferences
        editor.putString(Keys.SELECTED_ICON_TYPE, Defaults.SELECTED_ICON_TYPE)
        editor.putBoolean(Keys.INFO_CARD_ALWAYS_EXPANDED, Defaults.INFO_CARD_ALWAYS_EXPANDED)
        editor.putBoolean(Keys.SHOW_HELP_CARD, Defaults.SHOW_HELP_CARD)
        editor.remove(Keys.INFO_CARD_ITEM_ORDER)
        
        // Reset info card visibility settings
        editor.putBoolean(Keys.SHOW_MANAGER_VERSION, Defaults.SHOW_MANAGER_VERSION)
        editor.putBoolean(Keys.SHOW_HOOK_MODE, Defaults.SHOW_HOOK_MODE)
        editor.putBoolean(Keys.SHOW_MOUNT_SYSTEM, Defaults.SHOW_MOUNT_SYSTEM)
        editor.putBoolean(Keys.SHOW_SUSFS_STATUS, Defaults.SHOW_SUSFS_STATUS)
        editor.putBoolean(Keys.SHOW_ZYGISK_STATUS, Defaults.SHOW_ZYGISK_STATUS)
        editor.putBoolean(Keys.SHOW_KERNEL_VERSION, Defaults.SHOW_KERNEL_VERSION)
        editor.putBoolean(Keys.SHOW_ANDROID_VERSION, Defaults.SHOW_ANDROID_VERSION)
        editor.putBoolean(Keys.SHOW_ABI, Defaults.SHOW_ABI)
        editor.putBoolean(Keys.SHOW_SELINUX_STATUS, Defaults.SHOW_SELINUX_STATUS)
        
        // Reset module settings
        editor.putBoolean(Keys.KEEP_MODULE_CARDS_EXPANDED, Defaults.KEEP_MODULE_CARDS_EXPANDED)
        editor.putBoolean(Keys.ENABLE_MODULE_BANNERS, Defaults.ENABLE_MODULE_BANNERS)
        editor.putBoolean(Keys.HIDE_MODULE_DETAILS, Defaults.HIDE_MODULE_DETAILS)
        
        // Reset superuser settings
        editor.putBoolean(Keys.INDIVIDUAL_APP_CARDS, Defaults.INDIVIDUAL_APP_CARDS)
        editor.putBoolean(Keys.HIDE_FAVORITES_AUTOMATICALLY, Defaults.HIDE_FAVORITES_AUTOMATICALLY)
        editor.putBoolean(Keys.DISABLE_FAVORITE_BUTTON, Defaults.DISABLE_FAVORITE_BUTTON)
        
        editor.apply()
        
        // Update state flows
        _iconTypeFlow.value = Defaults.SELECTED_ICON_TYPE
        _infoCardExpandedFlow.value = Defaults.INFO_CARD_ALWAYS_EXPANDED
        _showHelpCardFlow.value = Defaults.SHOW_HELP_CARD
    }
    
    /**
     * Composable function to observe icon type changes
     */
    @Composable
    fun rememberIconType(): String {
        val iconType by iconTypeFlow.collectAsState()
        return iconType
    }
    
    /**
     * Composable function to observe info card expanded state changes
     */
    @Composable
    fun rememberInfoCardExpanded(): Boolean {
        val expanded by infoCardExpandedFlow.collectAsState()
        return expanded
    }
    
    /**
     * Composable function to observe help card visibility changes
     */
    @Composable
    fun rememberShowHelpCard(): Boolean {
        val showHelpCard by showHelpCardFlow.collectAsState()
        return showHelpCard
    }
}