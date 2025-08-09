package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InfoCardSettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    // State variables for all info card settings
    var infoCardAlwaysExpanded by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_always_expanded", false))
    }
    var showManagerVersion by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_manager_version", true))
    }
    var showHookMode by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_hook_mode", true))
    }
    var showMountSystem by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_mount_system", true))
    }
    var showSusfsStatus by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_susfs_status", true))
    }
    var showZygiskStatus by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_zygisk_status", true))
    }
    var showKernelVersion by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_kernel_version", true))
    }
    var showAndroidVersion by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_android_version", true))
    }
    var showAbi by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_abi", true))
    }
    var showSelinuxStatus by rememberSaveable {
        mutableStateOf(prefs.getBoolean("info_card_show_selinux_status", true))
    }

    // InfoCard items data class
    data class InfoCardItem(
        val key: String,
        val titleRes: Int,
        val enabled: Boolean,
        val onToggle: (Boolean) -> Unit,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val infoCardItems = remember(
        showManagerVersion, showHookMode, showMountSystem, showSusfsStatus,
        showZygiskStatus, showKernelVersion, showAndroidVersion, showAbi, showSelinuxStatus
    ) {
        listOf(
            InfoCardItem("manager_version", R.string.info_card_show_manager_version, showManagerVersion, {
                prefs.edit().putBoolean("info_card_show_manager_version", it).apply()
                showManagerVersion = it
            }, Icons.Filled.Apps),
            InfoCardItem("hook_mode", R.string.info_card_show_hook_mode, showHookMode, {
                prefs.edit().putBoolean("info_card_show_hook_mode", it).apply()
                showHookMode = it
            }, Icons.Filled.Phishing),
            InfoCardItem("mount_system", R.string.info_card_show_mount_system, showMountSystem, {
                prefs.edit().putBoolean("info_card_show_mount_system", it).apply()
                showMountSystem = it
            }, Icons.Filled.Storage),
            InfoCardItem("susfs_status", R.string.info_card_show_susfs_status, showSusfsStatus, {
                prefs.edit().putBoolean("info_card_show_susfs_status", it).apply()
                showSusfsStatus = it
            }, Icons.Filled.Security),
            InfoCardItem("zygisk_status", R.string.info_card_show_zygisk_status, showZygiskStatus, {
                prefs.edit().putBoolean("info_card_show_zygisk_status", it).apply()
                showZygiskStatus = it
            }, Icons.Filled.Android),
            InfoCardItem("kernel_version", R.string.info_card_show_kernel_version, showKernelVersion, {
                prefs.edit().putBoolean("info_card_show_kernel_version", it).apply()
                showKernelVersion = it
            }, Icons.Filled.Computer),
            InfoCardItem("android_version", R.string.info_card_show_android_version, showAndroidVersion, {
                prefs.edit().putBoolean("info_card_show_android_version", it).apply()
                showAndroidVersion = it
            }, Icons.Filled.PhoneAndroid),
            InfoCardItem("abi", R.string.info_card_show_abi, showAbi, {
                prefs.edit().putBoolean("info_card_show_abi", it).apply()
                showAbi = it
            }, Icons.Filled.Architecture),
            InfoCardItem("selinux_status", R.string.info_card_show_selinux_status, showSelinuxStatus, {
                prefs.edit().putBoolean("info_card_show_selinux_status", it).apply()
                showSelinuxStatus = it
            }, Icons.Filled.Shield)
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
        mutableStateOf(currentOrder)
    }

    // Save order when it changes
    LaunchedEffect(itemOrder) {
        prefs.edit().putString("info_card_items_order", itemOrder.joinToString(",")).apply()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                onBack = { navigator.navigateUp() },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                            Column(modifier = Modifier.weight(1f)) {
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
            itemsIndexed(itemOrder) { index, itemKey ->
                val item = infoCardItems.find { it.key == itemKey }
                if (item != null) {
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
                            // Icon
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
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
                                // Move up button
                                IconButton(
                                    onClick = { 
                                        if (index > 0) {
                                            val newOrder = itemOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index - 1]
                                            newOrder[index - 1] = temp
                                            itemOrder = newOrder
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowUp, 
                                        "Move up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Move down button  
                                IconButton(
                                    onClick = { 
                                        if (index < itemOrder.size - 1) {
                                            val newOrder = itemOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index + 1]
                                            newOrder[index + 1] = temp
                                            itemOrder = newOrder
                                        }
                                    },
                                    enabled = index < itemOrder.size - 1
                                ) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown, 
                                        "Move down",
                                        tint = if (index < itemOrder.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    TopAppBar(
        title = { 
            Text(
                text = stringResource(R.string.info_card_customization),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) 
        }, 
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) 
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}

@Preview
@Composable
private fun InfoCardSettingsPreview() {
    InfoCardSettingsScreen(EmptyDestinationsNavigator)
}