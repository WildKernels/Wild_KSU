package com.twj.wksu.ui.screen

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.system.Os
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.twj.wksu.*
import com.twj.wksu.R
import com.twj.wksu.ui.component.rememberConfirmDialog
import com.twj.wksu.ui.util.*
import com.twj.wksu.ui.util.IconUtils
import com.twj.wksu.ui.util.module.LatestVersionInfo
import com.twj.wksu.ui.theme.getCardElevation
import com.twj.wksu.ui.component.*
import com.twj.wksu.ui.component.CardConstants

import androidx.compose.material3.ElevatedCard

import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isManager = Natives.becomeManager(ksuApp.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)
    val showHelpCard = prefs.getBoolean("show_help_card", true)
    val selectedLayoutType = prefs.getString("home_layout_type", "STOCK") ?: "STOCK"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CardConstants.CARD_SPACING),
        verticalArrangement = Arrangement.spacedBy(CardConstants.CARD_SPACING)
    ) {
        // Only show StatusCard in STOCK layout, not in MIUIX layouts
        if (selectedLayoutType == "STOCK") {
            item {
                val lkmMode = ksuVersion?.let {
                    if (it >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && kernelVersion.isGKI()) Natives.isLkmMode else null
                }

                StatusCard(kernelVersion, ksuVersion, lkmMode) {
                    navigator.navigate(InstallScreenDestination)
                }
            }
        }

        // Only show this item if there's content to display
        val debugWarningCard = prefs.getBoolean("debug_warning_card", false)
        val debugUpdateCard = prefs.getBoolean("debug_update_card", false)
        val checkUpdate = prefs.getBoolean("check_update", false)
        
        val hasCardContent = (ksuVersion != null && rootAvailable()) ||
                            (debugWarningCard || (isManager && Natives.requireNewKernel())) ||
                            (debugWarningCard || (ksuVersion != null && !rootAvailable())) ||
                            (debugUpdateCard || checkUpdate)
        
        if (hasCardContent) {
            item {
                CardItemsColumn {
                    if (ksuVersion != null && rootAvailable()) {
                        if (selectedLayoutType == "MIUIX_SQUARE" || selectedLayoutType == "MIUIX_RECTANGLE") {
                            // MIUIX Layout: Custom StatusCard design
                            val lkmMode = ksuVersion.let {
                                if (it >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && kernelVersion.isGKI()) Natives.isLkmMode else null
                            }
                            MiuixStatusCard(
                                ksuVersion = ksuVersion,
                                kernelVersion = kernelVersion,
                                lkmMode = lkmMode,
                                layoutMode = selectedLayoutType,

                                onClickSuperuser = {
                                    navigator.navigate(SuperUserScreenDestination) {
                                        popUpTo(NavGraphs.root) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onClickModule = {
                                    navigator.navigate(ModuleScreenDestination) {
                                        popUpTo(NavGraphs.root) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        } else {
                            // STOCK Layout: Side-by-side arrangement
                            CardRow(
                                modifier = Modifier.height(IntrinsicSize.Min)
                            ) {
                                Box(modifier = Modifier.weight(1f)) { 
                                    SuperuserCard(onClick = { 
                                        navigator.navigate(SuperUserScreenDestination) {
                                            popUpTo(NavGraphs.root) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }) 
                                }
                                Box(modifier = Modifier.weight(1f)) { 
                                    ModuleCard(onClick = { 
                                        navigator.navigate(ModuleScreenDestination) {
                                            popUpTo(NavGraphs.root) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }) 
                                }
                            }
                        }
                    }

                    if (debugWarningCard || (isManager && Natives.requireNewKernel())) {
                        WarningCard(
                            stringResource(id = R.string.require_kernel_version).format(
                                ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL
                            )
                        )
                    }

                    if (debugWarningCard || (ksuVersion != null && !rootAvailable())) {
                        WarningCard(
                            stringResource(id = R.string.grant_root_failed)
                        )
                    }

                    if (debugUpdateCard || checkUpdate) {
                        UpdateCard()
                    }
                }
            }
        }

        item {
            InfoCard(autoExpand = developerOptionsEnabled)
        }

        if (showHelpCard) {
            item {
                CardItemsColumn {
                    IssueReportCard()
                }
            }
        }
    }
}

@Composable
private fun SuperuserCard(onClick: () -> Unit = {}) {
    val count = getSuperuserCount()
    
    CompactCard(
        cardType = CardType.SECONDARY,
        onClick = onClick
    ) {
        CenteredCardContent(
            title = if (count <= 1) {
                stringResource(R.string.home_superuser_count_singular)
            } else {
                stringResource(R.string.home_superuser_count_plural)
            },
            subtitle = count.toString()
        )
    }
}

@Composable
private fun ModuleCard(onClick: () -> Unit = {}) {
    val count = getModuleCount()
    
    CompactCard(
        cardType = CardType.SECONDARY,
        onClick = onClick
    ) {
        CenteredCardContent(
            title = if (count <= 1) {
                stringResource(R.string.home_module_count_singular)
            } else {
                stringResource(R.string.home_module_count_plural)
            },
            subtitle = count.toString()
        )
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)
    
    // Check if debug mode is enabled
    val debugUpdateCard = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getBoolean("debug_update_card", false)

    AnimatedVisibility(
        visible = debugUpdateCard || newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        val message = if (debugUpdateCard && newVersionCode <= currentVersionCode) {
            "Debug: Update card forced to show (current version: $currentVersionCode, latest: $newVersionCode)"
        } else {
            stringResource(id = R.string.new_version_available).format(newVersionCode)
        }
        
        WarningCard(
            message = message,
            MaterialTheme.colorScheme.outlineVariant
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title,
                    content = changelog,
                    markdown = true,
                    confirm = updateText
                )
            }
        }
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}







@Composable
private fun StatusCard(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?,
    moduleUpdateCount: Int = 0,
    onClickInstall: () -> Unit = {}
) {
    val context = LocalContext.current
    var tapCount by remember { mutableStateOf(0) }
    
    val clickHandler = {
        tapCount++
        if (tapCount == 5) {
            Toast.makeText(context, "What are you doing? 🤔", Toast.LENGTH_SHORT).show()
        } else if (tapCount == 10) {
            Toast.makeText(context, "Never gonna give you up! 💜", Toast.LENGTH_SHORT).show()
            val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (ksuVersion != null) {
                context.startActivity(intent)
            } else if (kernelVersion.isGKI()) {
                onClickInstall()
            } else {
                Toast.makeText(context, "Something weird happened... 🤔", Toast.LENGTH_SHORT).show()
            }
        } else if (ksuVersion == null && kernelVersion.isGKI()) {
            onClickInstall()
        }
    }
    
    StandardCard(
        cardType = CardType.CUSTOM,
        customColor = if (ksuVersion != null) MaterialTheme.colorScheme.primaryContainer
                     else MaterialTheme.colorScheme.errorContainer,
        onClick = clickHandler
    ) {
        StatusCardContent(kernelVersion, ksuVersion, lkmMode, moduleUpdateCount)
    }
}

@Composable
private fun StatusCardContent(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?,
    moduleUpdateCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
            when {
                ksuVersion != null -> {
                    val workingMode = when {
                        lkmMode == true -> "LKM"
                        lkmMode == false || kernelVersion.isGKI() -> "GKI2"
                        lkmMode == null && kernelVersion.isULegacy() -> "U-LEGACY"
                        lkmMode == null && kernelVersion.isLegacy() -> "LEGACY"
                        lkmMode == null && kernelVersion.isGKI1() -> "GKI1"
                        else -> "NON-STANDARD"
                    }

                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.home_working)
                    )
                    Column(
                        modifier = Modifier.padding(start = CardConstants.ICON_TO_TEXT_SPACING),
                        verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_SMALL)
                    ) {
                        val labelStyle = LabelItemDefaults.style
                        TextRow(
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CardConstants.ACTION_SPACING)
                                ) {
                                    LabelItem(
                                        icon = if (Natives.isSafeMode) {
                                            {
                                                Icon(
                                                    tint = labelStyle.contentColor,
                                                    imageVector = Icons.Filled.Security,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        text = {
                                            Text(
                                                text = workingMode,
                                                style = labelStyle.textStyle.copy(color = labelStyle.contentColor),
                                            )
                                        }
                                    )
                                    if (isSuCompatDisabled()) {
                                        LabelItem(
                                            icon = {
                                                Icon(
                                                    tint = labelStyle.contentColor,
                                                    imageVector = Icons.Filled.Warning,
                                                    contentDescription = null
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.sucompat_disabled),
                                                    style = labelStyle.textStyle.copy(
                                                        color = labelStyle.contentColor,
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_working),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = stringResource(R.string.home_working_version, ksuVersion),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                kernelVersion.isGKI() -> {
                    Icon(Icons.Filled.NewReleases, stringResource(R.string.home_not_installed))
                    Column(Modifier.padding(start = CardConstants.ICON_TO_TEXT_SPACING)) {
                        Text(
                            text = stringResource(R.string.home_not_installed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(CardConstants.ITEM_SPACING_SMALL))
                        Text(
                            text = stringResource(R.string.home_click_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Icon(Icons.Filled.Cancel, stringResource(R.string.home_failure))
                    Column(Modifier.padding(start = CardConstants.ICON_TO_TEXT_SPACING)) {
                        Text(
                            text = stringResource(R.string.home_failure),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(CardConstants.ITEM_SPACING_SMALL))
                        Text(
                            text = stringResource(R.string.home_failure_tip),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    StandardCard(
        cardType = CardType.CUSTOM,
        customColor = color,
        onClick = onClick
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoCard(autoExpand: Boolean = false) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Get customization preferences with reactive state to trigger recomposition
    var alwaysExpanded by remember { mutableStateOf(prefs.getBoolean("info_card_always_expanded", false)) }
    var showManagerVersion by remember { mutableStateOf(prefs.getBoolean("info_card_show_manager_version", true)) }
    var showHookMode by remember { mutableStateOf(prefs.getBoolean("info_card_show_hook_mode", true)) }
    var showMountSystem by remember { mutableStateOf(prefs.getBoolean("info_card_show_mount_system", true)) }
    var showSusfsStatus by remember { mutableStateOf(prefs.getBoolean("info_card_show_susfs_status", true)) }
    var showZygiskStatus by remember { mutableStateOf(prefs.getBoolean("info_card_show_zygisk_status", true)) }
    var showKernelVersion by remember { mutableStateOf(prefs.getBoolean("info_card_show_kernel_version", true)) }
    var showAndroidVersion by remember { mutableStateOf(prefs.getBoolean("info_card_show_android_version", true)) }
    var showAbi by remember { mutableStateOf(prefs.getBoolean("info_card_show_abi", true)) }
    var showSelinuxStatus by remember { mutableStateOf(prefs.getBoolean("info_card_show_selinux_status", true)) }
    
    // Get saved item order with state to trigger recomposition
    var itemOrder by remember {
        val savedOrder = prefs.getString("info_card_items_order", null)
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
            val result = saved.filter { key -> defaultOrder.contains(key) }.toMutableList()
            defaultOrder.forEach { key ->
                if (!result.contains(key)) result.add(key)
            }
            result
        }
        mutableStateOf(currentOrder)
    }
    
    // Listen for preference changes to update the order and visibility states
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "info_card_items_order" -> {
                    val newSavedOrder = prefs.getString("info_card_items_order", null)
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
                    itemOrder = newOrder
                }
                "info_card_always_expanded" -> {
                    alwaysExpanded = prefs.getBoolean("info_card_always_expanded", false)
                }
                "info_card_show_manager_version" -> {
                    showManagerVersion = prefs.getBoolean("info_card_show_manager_version", true)
                }
                "info_card_show_hook_mode" -> {
                    showHookMode = prefs.getBoolean("info_card_show_hook_mode", true)
                }
                "info_card_show_mount_system" -> {
                    showMountSystem = prefs.getBoolean("info_card_show_mount_system", true)
                }
                "info_card_show_susfs_status" -> {
                    showSusfsStatus = prefs.getBoolean("info_card_show_susfs_status", true)
                }
                "info_card_show_zygisk_status" -> {
                    showZygiskStatus = prefs.getBoolean("info_card_show_zygisk_status", true)
                }
                "info_card_show_kernel_version" -> {
                    showKernelVersion = prefs.getBoolean("info_card_show_kernel_version", true)
                }
                "info_card_show_android_version" -> {
                    showAndroidVersion = prefs.getBoolean("info_card_show_android_version", true)
                }
                "info_card_show_abi" -> {
                    showAbi = prefs.getBoolean("info_card_show_abi", true)
                }
                "info_card_show_selinux_status" -> {
                    showSelinuxStatus = prefs.getBoolean("info_card_show_selinux_status", true)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isManager = Natives.becomeManager(ksuApp.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    var expanded by rememberSaveable { mutableStateOf(false) }

    val developerOptionsEnabled by observePreferenceAsState(prefs, "enable_developer_options", false)

    // Count enabled options
    val enabledOptionsCount = listOf(
        showManagerVersion,
        showHookMode,
        showMountSystem,
        showSusfsStatus,
        showZygiskStatus,
        showKernelVersion,
        showAndroidVersion,
        showAbi,
        showSelinuxStatus
    ).count { it }

    // Don't render the card if no options are enabled
    if (enabledOptionsCount == 0) {
        return
    }

    LaunchedEffect(autoExpand, alwaysExpanded) {
        if (autoExpand || alwaysExpanded) {
            expanded = true
        }
    }   

    StandardCard(
        cardType = CardType.SURFACE,
        modifier = Modifier
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            ),
        onLongClick = {
            if (expanded && !alwaysExpanded) {
                expanded = false
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    ) {
        InfoCardContent(
            showManagerVersion = showManagerVersion,
            showHookMode = showHookMode,
            showMountSystem = showMountSystem,
            showSusfsStatus = showSusfsStatus,
            showZygiskStatus = showZygiskStatus,
            showKernelVersion = showKernelVersion,
            showAndroidVersion = showAndroidVersion,
            showAbi = showAbi,
            showSelinuxStatus = showSelinuxStatus,
            itemOrder = itemOrder,
            expanded = expanded,
            alwaysExpanded = alwaysExpanded,
            enabledOptionsCount = enabledOptionsCount,
            ksuVersion = ksuVersion,
            developerOptionsEnabled = developerOptionsEnabled,
            onExpandedChange = { expanded = it }
        )
    }
}

@Composable
private fun InfoCardContent(
    showManagerVersion: Boolean,
    showHookMode: Boolean,
    showMountSystem: Boolean,
    showSusfsStatus: Boolean,
    showZygiskStatus: Boolean,
    showKernelVersion: Boolean,
    showAndroidVersion: Boolean,
    showAbi: Boolean,
    showSelinuxStatus: Boolean,
    itemOrder: List<String>,
    expanded: Boolean,
    alwaysExpanded: Boolean,
    enabledOptionsCount: Int,
    ksuVersion: Int?,
    developerOptionsEnabled: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
            
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                CardItem(
                    label = label,
                    content = content,
                    icon = icon
                )
            }

            @Composable
            fun RenderInfoCardItem(itemKey: String, isFirst: Boolean) {
                val uname = Os.uname()
                when (itemKey) {
                    "info_card_show_manager_version" -> {
                        if (showManagerVersion) {
                            val managerVersion = getManagerVersion(context)
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_manager_version),
                                content = if (
                                    developerOptionsEnabled &&
                                    Natives.version >= Natives.MINIMAL_SUPPORTED_MANAGER_UID
                                ) {
                                    "${managerVersion.first} (${managerVersion.second}) | UID: ${Natives.getManagerUid()}"
                                } else {
                                    "${managerVersion.first} (${managerVersion.second})"
                                },
                                icon = painterResource(R.drawable.ic_ksu_next),
                            )
                        }
                    }
                    "info_card_show_hook_mode" -> {
                        if (showHookMode && ksuVersion != null &&
                            Natives.version >= Natives.MINIMAL_SUPPORTED_HOOK_MODE) {
                            val hookMode =
                                Natives.getHookMode()
                                    .takeUnless { it.isNullOrBlank() }
                                    ?: stringResource(R.string.unavailable)
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label   = stringResource(R.string.hook_mode),
                                content = hookMode,
                                icon    = Icons.Filled.Phishing,
                            )
                        }
                    }
                    "info_card_show_mount_system" -> {
                        if (showMountSystem && ksuVersion != null) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_mount_system),
                                content = currentMountSystem().ifEmpty { stringResource(R.string.unavailable) },
                                icon = Icons.Filled.SettingsSuggest,
                            )
                        }
                    }
                    "info_card_show_susfs_status" -> {
                        if (showSusfsStatus && ksuVersion != null) {
                            val suSFS = getSuSFS()
                            if (suSFS == "Supported") {
                                val isSUS_SU = hasSuSFs_SUS_SU() == "Supported"
                                val susSUMode = if (isSUS_SU) {
                                    val mode = susfsSUS_SU_Mode()
                                    val modeString =
                                        if (mode == "2") stringResource(R.string.enabled) else stringResource(R.string.disabled)
                                    "| SuS SU: $modeString"
                                } else ""
                                if (!isFirst) CardItemSpacer()
                                InfoCardItem(
                                    label = stringResource(R.string.home_susfs_version),
                                    content = "${stringResource(R.string.susfs_supported)} | ${getSuSFSVersion()} (${getSuSFSVariant()}) $susSUMode",
                                    icon = painterResource(R.drawable.ic_sus),
                                )
                            }
                        }
                    }
                    "info_card_show_zygisk_status" -> {
                        if (showZygiskStatus && ksuVersion != null && Natives.isZygiskEnabled()) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.zygisk_status),
                                content = stringResource(R.string.enabled),
                                icon = Icons.Filled.Vaccines
                            )
                        }
                    }
                    "info_card_show_kernel_version" -> {
                        if (showKernelVersion) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_kernel),
                                content = "${uname.release} (${uname.machine})",
                                icon = painterResource(R.drawable.ic_linux),
                            )
                        }
                    }
                    "info_card_show_android_version" -> {
                        if (showAndroidVersion) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_android),
                                content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
                                icon = Icons.Filled.Android,
                            )
                        }
                    }
                    "info_card_show_abi" -> {
                        if (showAbi) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_abi),
                                content = Build.SUPPORTED_ABIS.joinToString(", "),
                                icon = Icons.Filled.Memory,
                            )
                        }
                    }
                    "info_card_show_selinux_status" -> {
                        if (showSelinuxStatus) {
                            if (!isFirst) CardItemSpacer()
                            InfoCardItem(
                                label = stringResource(R.string.home_selinux_status),
                                content = getSELinuxStatus(),
                                icon = Icons.Filled.Security,
                            )
                        }
                    }
                }
            }

            Column {
                // Render items in the saved order, limiting to 5 unless expanded
                var isFirstItem = true
                var renderedCount = 0
                val maxItemsWhenCollapsed = 5
                var hasMoreItems = false
                
                itemOrder.forEach { itemKey ->
                    // Check if the item would be rendered
                    val itemWouldBeRendered = when (itemKey) {
                        "info_card_show_manager_version" -> showManagerVersion
                        "info_card_show_hook_mode" -> showHookMode && ksuVersion != null && Natives.version >= Natives.MINIMAL_SUPPORTED_HOOK_MODE
                        "info_card_show_mount_system" -> showMountSystem && ksuVersion != null
                        "info_card_show_susfs_status" -> showSusfsStatus && ksuVersion != null && getSuSFS() == "Supported"
                        "info_card_show_zygisk_status" -> showZygiskStatus && ksuVersion != null && Natives.isZygiskEnabled()
                        "info_card_show_kernel_version" -> showKernelVersion
                        "info_card_show_android_version" -> showAndroidVersion
                        "info_card_show_abi" -> showAbi
                        "info_card_show_selinux_status" -> showSelinuxStatus
                        else -> false
                    }
                    
                    if (itemWouldBeRendered) {
                        // Show item if we're expanded/alwaysExpanded or haven't reached the limit
                        val shouldShowItem = expanded || alwaysExpanded || renderedCount < maxItemsWhenCollapsed
                        
                        if (shouldShowItem) {
                            val wasFirstItem = isFirstItem
                            val isExtraItem = renderedCount >= maxItemsWhenCollapsed
                            
                            // Show all items without wrapper to fix spacing issues
                            RenderInfoCardItem(itemKey, wasFirstItem)
                            
                            if (isFirstItem) {
                                isFirstItem = false
                            }
                            renderedCount++
                        } else {
                            // There are more items that could be shown
                            hasMoreItems = true
                        }
                    }
                }
                
                // Show expand button only when collapsed, not alwaysExpanded, and there are 5+ enabled options
                AnimatedVisibility(
                    visible = !expanded && !alwaysExpanded && enabledOptionsCount >= 5,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Column {
                        CardItemSpacer()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { onExpandedChange(true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Show more"
                                )
                            }
                        }
                    }
                }
            }
        }





// Icon helper functions moved to IconUtils

@Composable
fun IssueReportCard() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val selectedIconType = prefs.getString("selected_icon_type", "SEASONAL") ?: "SEASONAL"
    
    // Get the appropriate icon using IconUtils
    val helpIcon = IconUtils.getIcon(selectedIconType)
    
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)

    StandardCard(
        cardType = CardType.SURFACE
    ) {
        IssueReportCardContent(
            uriHandler = uriHandler,
            githubIssueUrl = githubIssueUrl,
            telegramUrl = telegramUrl
        )
    }
}

@Composable
private fun IssueReportCardContent(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    githubIssueUrl: String,
    telegramUrl: String
) {
    Column {
        CardRowContent(
            text = stringResource(R.string.issue_report_body),
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = stringResource(R.string.issue_report_title),
            subtitle = stringResource(R.string.issue_report_body_2),
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(CardConstants.ACTION_SPACING)) {
                    IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = stringResource(R.string.issue_report_github),
                        )
                    }
                    IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_telegram),
                            contentDescription = stringResource(R.string.issue_report_telegram),
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MiuixStatusCard(
    ksuVersion: Int,
    kernelVersion: KernelVersion,
    lkmMode: Boolean?,
    layoutMode: String = "MIUIX_SQUARE",
    onClickSuperuser: () -> Unit = {},
    onClickModule: () -> Unit = {},
) {
    val safeMode = when {
        Natives.isSafeMode -> " [${stringResource(id = R.string.safe_mode)}]"
        else -> ""
    }
    
    val workingMode = when {
        lkmMode == true -> "LKM"
        lkmMode == false || kernelVersion.isGKI() -> "GKI2"
        lkmMode == null && kernelVersion.isULegacy() -> "U-LEGACY"
        lkmMode == null && kernelVersion.isLegacy() -> "LEGACY"
        lkmMode == null && kernelVersion.isGKI1() -> "GKI1"
        else -> "NON-STANDARD"
    }
    
    val workingText = "${stringResource(id = R.string.home_working)}$safeMode"
    
    // Calculate weights based on layout mode
    val (mainCardWeight, sideCardWeight) = when (layoutMode) {
        "MIUIX_RECTANGLE" -> 0.65f to 0.35f  // 65% main card, 35% side cards
        else -> 0.5f to 0.5f  // 50% each for square mode
    }
    
    // Horizontal layout: Main card on left, two smaller cards on right, both sides centered
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CardConstants.CARD_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main status card - responsive width based on layout mode
        Box(
            modifier = Modifier.weight(mainCardWeight),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (layoutMode == "MIUIX_RECTANGLE") 120.dp else 160.dp), // Fixed height for better control
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(15.dp, 20.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(70.dp),
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            tint = Color(0xFF36D167),
                            contentDescription = null
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = workingText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val labelStyle = LabelItemDefaults.style
                        LabelItem(
                            icon = if (Natives.isSafeMode) {
                                {
                                    Icon(
                                        tint = labelStyle.contentColor,
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null
                                    )
                                }
                            } else {
                                null
                            },
                            text = {
                                Text(
                                    text = workingMode,
                                    style = labelStyle.textStyle.copy(color = labelStyle.contentColor),
                                )
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.home_working_version, ksuVersion),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        
        // Right side: Two smaller cards stacked vertically - responsive to available space
        Box(
            modifier = Modifier.weight(sideCardWeight),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(CardConstants.CARD_SPACING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp), // Fixed height that scales with available width
                    onClick = onClickSuperuser
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.superuser),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = getSuperuserCount().toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp), // Fixed height that scales with available width
                    onClick = onClickModule
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.module),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = getModuleCount().toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column {
        StatusCard(KernelVersion(5, 10, 101), 1, null)
        StatusCard(KernelVersion(5, 10, 101), 20000, true)
        StatusCard(KernelVersion(5, 10, 101), null, true)
        StatusCard(KernelVersion(4, 10, 101), null, false)
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message ",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}
