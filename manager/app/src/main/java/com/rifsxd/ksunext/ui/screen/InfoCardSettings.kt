package com.rifsxd.ksunext.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun InfoCardSettingsScreen(
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
    @Composable
    fun getIconForItem(iconType: String, iconData: Any): @Composable () -> Unit = {
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

    val infoCardItems = remember(
        showManagerVersion, showHookMode, showMountSystem, showSusfsStatus,
        showZygiskStatus, showKernelVersion, showAndroidVersion, showAbi, showSelinuxStatus
    ) {
        listOf(
            InfoCardItem("manager_version", R.string.home_manager_version, showManagerVersion, {
                prefs.edit().putBoolean("info_card_show_manager_version", it).apply()
                showManagerVersion = it
            }, "drawable", R.drawable.ic_ksu_next),
            InfoCardItem("hook_mode", R.string.hook_mode, showHookMode, {
                prefs.edit().putBoolean("info_card_show_hook_mode", it).apply()
                showHookMode = it
            }, "vector", Icons.Filled.Phishing),
            InfoCardItem("mount_system", R.string.home_mount_system, showMountSystem, {
                prefs.edit().putBoolean("info_card_show_mount_system", it).apply()
                showMountSystem = it
            }, "vector", Icons.Filled.SettingsSuggest),
            InfoCardItem("susfs_status", R.string.home_susfs_version, showSusfsStatus, {
                prefs.edit().putBoolean("info_card_show_susfs_status", it).apply()
                showSusfsStatus = it
            }, "drawable", R.drawable.ic_sus),
            InfoCardItem("zygisk_status", R.string.zygisk_status, showZygiskStatus, {
                prefs.edit().putBoolean("info_card_show_zygisk_status", it).apply()
                showZygiskStatus = it
            }, "vector", Icons.Filled.Vaccines),
            InfoCardItem("kernel_version", R.string.home_kernel, showKernelVersion, {
                prefs.edit().putBoolean("info_card_show_kernel_version", it).apply()
                showKernelVersion = it
            }, "drawable", R.drawable.ic_linux),
            InfoCardItem("android_version", R.string.home_android, showAndroidVersion, {
                prefs.edit().putBoolean("info_card_show_android_version", it).apply()
                showAndroidVersion = it
            }, "vector", Icons.Filled.Android),
            InfoCardItem("abi", R.string.home_abi, showAbi, {
                prefs.edit().putBoolean("info_card_show_abi", it).apply()
                showAbi = it
            }, "vector", Icons.Filled.Memory),
            InfoCardItem("selinux_status", R.string.home_selinux_status, showSelinuxStatus, {
                prefs.edit().putBoolean("info_card_show_selinux_status", it).apply()
                showSelinuxStatus = it
            }, "vector", Icons.Filled.Security)
        )
    }

    // Get current order from preferences
    var itemOrder by remember {
        val savedOrder = prefs.getString("info_card_items_order", "")
        val defaultOrder = infoCardItems.map { it.key }
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

    // Icon Selection with OFF option
    val iconOptions = listOf(
        "OFF" to "Off",
        "SEASONAL" to "Seasonal (Auto)",
        "WINTER" to "Winter",
        "SPRING" to "Spring", 
        "SUMMER" to "Summer",
        "FALL" to "Fall",
        "KSU_NEXT" to "KSU Next",
        "CANNABIS" to "Cannabis"
    )
    
    val currentIconDisplay = iconOptions.find { it.first == selectedIconType }?.second ?: "Seasonal (Auto)"
    
    val iconDialog = rememberCustomDialog { dismiss ->
        val options = iconOptions.map { (value, display) ->
            ListOption(
                titleText = display,
                selected = value == selectedIconType
            )
        }
        
        ListDialog(
            state = rememberUseCaseState(visible = true, onCloseRequest = { dismiss() }),
            header = Header.Default(title = stringResource(R.string.home_screen_icon_select_dialog_title)),
            selection = ListSelection.Single(
                showRadioButtons = true,
                options = options
            ) { index, _ ->
                val selectedIcon = iconOptions[index].first
                prefs.edit().putString("selected_icon_type", selectedIcon).apply()
                selectedIconType = selectedIcon
                dismiss()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Always Expanded Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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

            // Help Card Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .clickable { 
                                val newValue = !showHelpCard
                                prefs.edit().putBoolean("show_help_card", newValue).apply()
                                showHelpCard = newValue
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Help,
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
            }

            // Home Screen Icon Style
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .clickable {
                                iconDialog.show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            }

            // Section header
            item {
                Text(
                    text = "System Information Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Info card items
            itemsIndexed(
                items = itemOrder,
                key = { _, itemKey -> itemKey }
            ) { index, itemKey ->
                val item = infoCardItems.find { it.key == itemKey }
                if (item != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            getIconForItem(item.iconType, item.iconData)()
                            
                            // Title
                            Text(
                                text = stringResource(item.titleRes),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move up button (tap = move up 1, long press = move to top)
                                ReorderButton(
                                    icon = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    enabled = index > 0,
                                    onTap = {
                                        if (index > 0) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newOrder = itemOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index - 1]
                                            newOrder[index - 1] = temp
                                            itemOrder = newOrder
                                        }
                                    },
                                    onLongPress = {
                                        if (index > 0) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newOrder = itemOrder.toMutableList()
                                            val item = newOrder.removeAt(index)
                                            newOrder.add(0, item)
                                            itemOrder = newOrder
                                        }
                                    }
                                )
                                
                                // Move down button (tap = move down 1, long press = move to bottom)
                                ReorderButton(
                                    icon = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    enabled = index < itemOrder.size - 1,
                                    onTap = {
                                        if (index < itemOrder.size - 1) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newOrder = itemOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index + 1]
                                            newOrder[index + 1] = temp
                                            itemOrder = newOrder
                                        }
                                    },
                                    onLongPress = {
                                        if (index < itemOrder.size - 1) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newOrder = itemOrder.toMutableList()
                                            val item = newOrder.removeAt(index)
                                            newOrder.add(item)
                                            itemOrder = newOrder
                                        }
                                    }
                                )
                                
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

@Composable
private fun ReorderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            val released = try {
                                tryAwaitRelease()
                            } catch (e: Exception) {
                                false
                            }
                            isPressed = false
                            released
                        },
                        onTap = { onTap() },
                        onLongPress = { onLongPress() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview
@Composable
private fun InfoCardSettingsPreview() {
    InfoCardSettingsScreen(EmptyDestinationsNavigator)
}