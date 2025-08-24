package com.rifsxd.ksunext.ui.screen

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import kotlinx.coroutines.delay
import java.util.*
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.util.IconUtils

// Icon helper functions moved to IconUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun HomeSettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val hapticFeedback = LocalHapticFeedback.current
    
    // State variables for all info card settings
    var infoCardAlwaysExpanded by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_always_expanded", false))
    }
    
    // Help card toggle state
    var showHelpCard by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("show_help_card", true))
    }
    
    // Home screen icon style state
    var selectedIconType by rememberSaveable {
        mutableStateOf(
            prefs.getString("selected_icon_type", "SEASONAL") ?: "SEASONAL"
        )
    }
    var showManagerVersion by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_manager_version", true))
    }
    var showHookMode by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_hook_mode", true))
    }
    var showMountSystem by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_mount_system", true))
    }
    var showSusfsStatus by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_susfs_status", true))
    }
    var showZygiskStatus by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_zygisk_status", true))
    }
    var showKernelVersion by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_kernel_version", true))
    }
    var showAndroidVersion by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_android_version", true))
    }
    var showAbi by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_abi", true))
    }
    var showSelinuxStatus by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("info_card_show_selinux_status", true))
    }
    
    // Card background toggle state
    var cardBackgroundEnabled by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("card_background_enabled", true))
    }
    
    // App name customization state
    var selectedAppName by rememberSaveable {
        mutableStateOf(prefs.getString("selected_app_name", "kernelsu_next") ?: "kernelsu_next")
    }
    
    // Card debugging state variables
    var debugWarningCard by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("debug_warning_card", false))
    }
    var debugUpdateCard by rememberSaveable {
        mutableStateOf<Boolean>(prefs.getBoolean("debug_update_card", false))
    }
    
    val onSelectedAppNameChanged = { newAppName: String ->
        selectedAppName = newAppName
        prefs.edit().putString("selected_app_name", newAppName).apply()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // InfoCard items data class
    data class InfoCardItem(
        val key: String,
        val titleRes: Int,
        val enabled: Boolean,
        val onToggle: (Boolean) -> Unit,
        val iconType: String, // "vector" or "drawable"
        val iconData: Any // ImageVector or drawable resource ID
    )

    // Helper function to get icon composable
    val getIconForItem = { iconType: String, iconData: Any ->
        @Composable {
            when (iconType) {
                "vector" -> Icon(
                    imageVector = iconData as ImageVector,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                "drawable" -> Icon(
                    painter = painterResource(iconData as Int),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    val infoCardItems = remember(
        showManagerVersion, showHookMode, showMountSystem, showSusfsStatus,
        showZygiskStatus, showKernelVersion, showAndroidVersion, showAbi, showSelinuxStatus
    ) {
        listOf(
            InfoCardItem("info_card_show_manager_version", R.string.home_manager_version, showManagerVersion, {
                prefs.edit().putBoolean("info_card_show_manager_version", it).apply()
                showManagerVersion = it
            }, "drawable", R.drawable.ic_ksu_next),
            InfoCardItem("info_card_show_hook_mode", R.string.hook_mode, showHookMode, {
                prefs.edit().putBoolean("info_card_show_hook_mode", it).apply()
                showHookMode = it
            }, "vector", Icons.Filled.Phishing),
            InfoCardItem("info_card_show_mount_system", R.string.home_mount_system, showMountSystem, {
                prefs.edit().putBoolean("info_card_show_mount_system", it).apply()
                showMountSystem = it
            }, "vector", Icons.Filled.SettingsSuggest),
            InfoCardItem("info_card_show_susfs_status", R.string.home_susfs_version, showSusfsStatus, {
                prefs.edit().putBoolean("info_card_show_susfs_status", it).apply()
                showSusfsStatus = it
            }, "drawable", R.drawable.ic_sus),
            InfoCardItem("info_card_show_zygisk_status", R.string.zygisk_status, showZygiskStatus, {
                prefs.edit().putBoolean("info_card_show_zygisk_status", it).apply()
                showZygiskStatus = it
            }, "vector", Icons.Filled.Vaccines),
            InfoCardItem("info_card_show_kernel_version", R.string.home_kernel, showKernelVersion, {
                prefs.edit().putBoolean("info_card_show_kernel_version", it).apply()
                showKernelVersion = it
            }, "drawable", R.drawable.ic_linux),
            InfoCardItem("info_card_show_android_version", R.string.home_android, showAndroidVersion, {
                prefs.edit().putBoolean("info_card_show_android_version", it).apply()
                showAndroidVersion = it
            }, "vector", Icons.Filled.Android),
            InfoCardItem("info_card_show_abi", R.string.home_abi, showAbi, {
                prefs.edit().putBoolean("info_card_show_abi", it).apply()
                showAbi = it
            }, "vector", Icons.Filled.Memory),
            InfoCardItem("info_card_show_selinux_status", R.string.home_selinux_status, showSelinuxStatus, {
                prefs.edit().putBoolean("info_card_show_selinux_status", it).apply()
                showSelinuxStatus = it
            }, "vector", Icons.Filled.Security)
        )
    }

    // Get current order from preferences
    var itemOrder by remember {
        val savedOrder = prefs.getString("info_card_items_order", "")
        val defaultOrder = listOf(
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
        val currentOrder = if (savedOrder.isNullOrEmpty()) {
            defaultOrder
        } else {
            val saved = savedOrder.split(",")
            // Ensure all items are present and add any new ones
            val result = saved.filter { key -> defaultOrder.contains(key) }.toMutableList()
            defaultOrder.forEach { key ->
                if (!result.contains(key)) result.add(key)
            }
            result
        }
        mutableStateOf<List<String>>(currentOrder)
    }

    // Save order when it changes
    LaunchedEffect(itemOrder) {
        prefs.edit().putString("info_card_items_order", itemOrder.joinToString(",")).apply()
    }
    
    // Listen for external preference changes and update itemOrder
    LaunchedEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "info_card_items_order") {
                val newSavedOrder = prefs.getString("info_card_items_order", "")
                val defaultOrder = listOf(
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
                val newOrder = if (newSavedOrder.isNullOrEmpty()) {
                    defaultOrder
                } else {
                    val saved = newSavedOrder.split(",")
                    val result = saved.filter { key -> defaultOrder.contains(key) }.toMutableList()
                    defaultOrder.forEach { key ->
                        if (!result.contains(key)) result.add(key)
                    }
                    result
                }
                if (newOrder != itemOrder) {
                    itemOrder = newOrder
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    // Icon Selection with OFF option
    val iconOptions = listOf(
        "OFF" to "Off",
        "SEASONAL" to "Seasonal (Auto)",
        "WINTER" to "Winter",
        "SPRING" to "Spring", 
        "SUMMER" to "Summer",
        "FALL" to "Fall",
        "KSU_NEXT" to "KSU Next",
        "CANNABIS" to "Cannabis",
        "AMOGUS_SUSFS" to "Amungus"
    )
    
    val currentIconDisplay = iconOptions.find { it.first == selectedIconType }?.second ?: "Seasonal (Auto)"
    
    // State for showing icon selection dialog
    var showIconDialog by remember { mutableStateOf(false) }
    
    // Icon selection dialog with visual icons
    if (showIconDialog) {
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.home_screen_icon_select_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Scroll to see more icons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(iconOptions) { (value, display) ->
                        val isSelected = value == selectedIconType
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.edit().putString("selected_icon_type", value).apply()
                                    selectedIconType = value
                                    showIconDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) 
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Icon preview
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = IconUtils.getIcon(value)
                                    when (icon) {
                                        is ImageVector -> Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        is Painter -> Icon(
                                            painter = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                // Text
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = display,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (value == "SEASONAL") {
                                        Text(
                                            text = "Currently: ${IconUtils.getSeasonalIconName()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Selection indicator
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }


                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Home & Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_screen_settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Home Screen Icon Style
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showIconDialog = true
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Show current icon preview
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val currentIcon = IconUtils.getIcon(selectedIconType)
                                    when (currentIcon) {
                                        is ImageVector -> Icon(
                                            imageVector = currentIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        is Painter -> Icon(
                                            painter = currentIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_screen_icon_style),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.home_screen_icon_style_summary) + ". " + stringResource(R.string.home_screen_icon_current, currentIconDisplay),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Navigate to settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // App Name Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Title,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.app_name_customization),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Switch to Wild KSU",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = selectedAppName == "wild_ksu",
                                    onCheckedChange = { isChecked ->
                                        val newAppName = if (isChecked) "wild_ksu" else "kernelsu_next"
                                        onSelectedAppNameChanged(newAppName)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Background Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    val newValue = !cardBackgroundEnabled
                                    prefs.edit().putBoolean("card_background_enabled", newValue).apply()
                                    cardBackgroundEnabled = newValue
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CropFree,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Card Background",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Toggle card background and padding for expanded layout",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = cardBackgroundEnabled,
                                    onCheckedChange = {
                                        prefs.edit().putBoolean("card_background_enabled", it).apply()
                                        cardBackgroundEnabled = it
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Help Card Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    val newValue = !showHelpCard
                                    prefs.edit().putBoolean("show_help_card", newValue).apply()
                                    showHelpCard = newValue
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.help_card_customization),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.help_card_customization_summary),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = showHelpCard,
                                    onCheckedChange = {
                                        prefs.edit().putBoolean("show_help_card", it).apply()
                                        showHelpCard = it
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Always Expanded Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.info_card_always_expanded),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.info_card_always_expanded_summary),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = infoCardAlwaysExpanded,
                                    onCheckedChange = {
                                        prefs.edit().putBoolean("info_card_always_expanded", it).apply()
                                        infoCardAlwaysExpanded = it
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Card Debugging Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Card Debugging",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = "Enable or disable conditional cards on the home screen for debugging purposes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Warning Card Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    val newValue = !debugWarningCard
                                    prefs.edit().putBoolean("debug_warning_card", newValue).apply()
                                    debugWarningCard = newValue
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Warning Card",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Force show warning cards (root not available, kernel version)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = debugWarningCard,
                                    onCheckedChange = {
                                        prefs.edit().putBoolean("debug_warning_card", it).apply()
                                        debugWarningCard = it
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Update Card Toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    val newValue = !debugUpdateCard
                                    prefs.edit().putBoolean("debug_update_card", newValue).apply()
                                    debugUpdateCard = newValue
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Update,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Update Card",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Force show update card regardless of check_update setting",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = debugUpdateCard,
                                    onCheckedChange = {
                                        prefs.edit().putBoolean("debug_update_card", it).apply()
                                        debugUpdateCard = it
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Info Card Order Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.info_card_order_management),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = stringResource(R.string.info_card_order_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Info card items with tap to move up/down and hold to move to top/bottom
                        itemOrder.forEachIndexed { index, itemKey ->
                            val item = infoCardItems.find { it.key == itemKey }
                            if (item != null) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(300),
                                        initialOffsetY = { it / 2 }
                                    ) + fadeIn(animationSpec = tween(300)),
                                    exit = slideOutVertically(
                                        animationSpec = tween(300),
                                        targetOffsetY = { -it / 2 }
                                    ) + fadeOut(animationSpec = tween(300))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                            // Icon
                                getIconForItem(item.iconType, item.iconData)
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Title
                                Text(
                                    text = stringResource(item.titleRes),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Move controls - side by side with bigger circular buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Move up button (tap to move up one, hold to move to top)
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index > 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (index > 0) {
                                                        val newOrder = itemOrder.toMutableList()
                                                        val temp = newOrder[index]
                                                        newOrder[index] = newOrder[index - 1]
                                                        newOrder[index - 1] = temp
                                                        itemOrder = newOrder
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (index > 0) {
                                                        // Move to top
                                                        val newOrder = itemOrder.toMutableList()
                                                        val itemToMove = newOrder.removeAt(index)
                                                        newOrder.add(0, itemToMove)
                                                        itemOrder = newOrder
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move up (tap) or to top (hold)",
                                            modifier = Modifier.size(30.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                    
                                    // Move down button (tap to move down one, hold to move to bottom)
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index < itemOrder.size - 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (index < itemOrder.size - 1) {
                                                        val newOrder = itemOrder.toMutableList()
                                                        val temp = newOrder[index]
                                                        newOrder[index] = newOrder[index + 1]
                                                        newOrder[index + 1] = temp
                                                        itemOrder = newOrder
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (index < itemOrder.size - 1) {
                                                        // Move to bottom
                                                        val newOrder = itemOrder.toMutableList()
                                                        val itemToMove = newOrder.removeAt(index)
                                                        newOrder.add(itemToMove)
                                                        itemOrder = newOrder
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move down (tap) or to bottom (hold)",
                                            modifier = Modifier.size(30.dp),
                                            tint = if (index < itemOrder.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Toggle switch
                                Switch(
                                    checked = item.enabled,
                                    onCheckedChange = item.onToggle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    } // Added missing closing brace for main LazyColumn
}

@Preview
@Composable
fun HomeSettingsPreview() {
    HomeSettingsScreen(EmptyDestinationsNavigator)
}