package com.rifsxd.ksunext.ui.screen

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
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.rifsxd.ksunext.*
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.util.module.LatestVersionInfo
import com.rifsxd.ksunext.ui.theme.getCardElevation

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val lkmMode = ksuVersion?.let {
                if (it >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && kernelVersion.isGKI()) Natives.isLkmMode else null
            }

            StatusCard(kernelVersion, ksuVersion, lkmMode) {
                navigator.navigate(InstallScreenDestination)
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (ksuVersion != null && rootAvailable()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                if (isManager && Natives.requireNewKernel()) {
                    WarningCard(
                        stringResource(id = R.string.require_kernel_version).format(
                            ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL
                        )
                    )
                }

                if (ksuVersion != null && !rootAvailable()) {
                    WarningCard(
                        stringResource(id = R.string.grant_root_failed)
                    )
                }

                val checkUpdate =
                    LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getBoolean("check_update", false)
                if (checkUpdate) {
                    UpdateCard()
                }
            }
        }

        item {
            InfoCard(autoExpand = developerOptionsEnabled)
        }

        if (showHelpCard) {
            item {
                IssueReportCard()
            }
        }
    }
}

@Composable
private fun SuperuserCard(onClick: () -> Unit = {}) {
    val count = getSuperuserCount()
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = getCardElevation(),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_superuser_count_singular)
                    } else {
                        stringResource(R.string.home_superuser_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(onClick: () -> Unit = {}) {
    val count = getModuleCount()
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = getCardElevation(),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_module_count_singular)
                    } else {
                        stringResource(R.string.home_module_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.new_version_available).format(newVersionCode),
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

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            if (ksuVersion != null) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        }),
        elevation = getCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
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
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        modifier = Modifier.padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labelStyle = LabelItemDefaults.style
                        TextRow(
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_not_installed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_click_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Icon(Icons.Filled.Cancel, stringResource(R.string.home_failure))
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_failure),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_failure_tip),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        ),
        elevation = getCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)
        ) {
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
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

    LaunchedEffect(autoExpand, alwaysExpanded) {
        if (autoExpand || alwaysExpanded) {
            expanded = true
        }
    }   

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = getCardElevation(),
        modifier = Modifier
            .clip(CardDefaults.elevatedShape)
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    if (expanded && !alwaysExpanded) {
                        expanded = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        when (icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            @Composable
            fun RenderInfoCardItem(itemKey: String, isFirst: Boolean) {
                when (itemKey) {
                    "info_card_show_manager_version" -> {
                        if (showManagerVersion) {
                            val managerVersion = getManagerVersion(context)
                            if (!isFirst) Spacer(Modifier.height(16.dp))
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
                            if (!isFirst) Spacer(Modifier.height(16.dp))
                            InfoCardItem(
                                label   = stringResource(R.string.hook_mode),
                                content = hookMode,
                                icon    = Icons.Filled.Phishing,
                            )
                        }
                    }
                    "info_card_show_mount_system" -> {
                        if (showMountSystem && ksuVersion != null) {
                            if (!isFirst) Spacer(Modifier.height(16.dp))
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
                                if (!isFirst) Spacer(Modifier.height(16.dp))
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
                            if (!isFirst) Spacer(Modifier.height(16.dp))
                            InfoCardItem(
                                label = stringResource(R.string.zygisk_status),
                                content = stringResource(R.string.enabled),
                                icon = Icons.Filled.Vaccines
                            )
                        }
                    }
                }
            }

            Column {
                // Render items in the saved order
                var isFirstItem = true
                itemOrder.forEach { itemKey ->
                    val wasFirstItem = isFirstItem
                    RenderInfoCardItem(itemKey, wasFirstItem)
                    // Check if the item was actually rendered by checking if any of the conditions were met
                    val itemWasRendered = when (itemKey) {
                        "info_card_show_manager_version" -> showManagerVersion
                        "info_card_show_hook_mode" -> showHookMode && ksuVersion != null && Natives.version >= Natives.MINIMAL_SUPPORTED_HOOK_MODE
                        "info_card_show_mount_system" -> showMountSystem && ksuVersion != null
                        "info_card_show_susfs_status" -> showSusfsStatus && ksuVersion != null && getSuSFS() == "Supported"
                        "info_card_show_zygisk_status" -> showZygiskStatus && ksuVersion != null && Natives.isZygiskEnabled()
                        else -> false
                    }
                    if (itemWasRendered && isFirstItem) {
                        isFirstItem = false
                    }
                }

                if (!expanded && !alwaysExpanded && enabledOptionsCount >= 5) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Show more"
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = expanded || alwaysExpanded) {
                    val uname = Os.uname()
                    Column {
                        // Render expanded items in the saved order
                        val expandedItems = listOf(
                            "info_card_show_kernel_version",
                            "info_card_show_android_version",
                            "info_card_show_abi",
                            "info_card_show_selinux_status"
                        )
                        
                        val orderedExpandedItems = itemOrder.filter { it in expandedItems }
                        
                        if (orderedExpandedItems.any { itemKey ->
                            when (itemKey) {
                                "info_card_show_kernel_version" -> showKernelVersion
                                "info_card_show_android_version" -> showAndroidVersion
                                "info_card_show_abi" -> showAbi
                                "info_card_show_selinux_status" -> showSelinuxStatus
                                else -> false
                            }
                        }) {
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        var isFirstExpandedItem = true
                        orderedExpandedItems.forEach { itemKey ->
                            when (itemKey) {
                                "info_card_show_kernel_version" -> {
                                    if (showKernelVersion) {
                                        if (!isFirstExpandedItem) Spacer(Modifier.height(16.dp))
                                        InfoCardItem(
                                            label = stringResource(R.string.home_kernel),
                                            content = "${uname.release} (${uname.machine})",
                                            icon = painterResource(R.drawable.ic_linux),
                                        )
                                        isFirstExpandedItem = false
                                    }
                                }
                                "info_card_show_android_version" -> {
                                    if (showAndroidVersion) {
                                        if (!isFirstExpandedItem) Spacer(Modifier.height(16.dp))
                                        InfoCardItem(
                                            label = stringResource(R.string.home_android),
                                            content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
                                            icon = Icons.Filled.Android,
                                        )
                                        isFirstExpandedItem = false
                                    }
                                }
                                "info_card_show_abi" -> {
                                    if (showAbi) {
                                        if (!isFirstExpandedItem) Spacer(Modifier.height(16.dp))
                                        InfoCardItem(
                                            label = stringResource(R.string.home_abi),
                                            content = Build.SUPPORTED_ABIS.joinToString(", "),
                                            icon = Icons.Filled.Memory,
                                        )
                                        isFirstExpandedItem = false
                                    }
                                }
                                "info_card_show_selinux_status" -> {
                                    if (showSelinuxStatus) {
                                        if (!isFirstExpandedItem) Spacer(Modifier.height(16.dp))
                                        InfoCardItem(
                                            label = stringResource(R.string.home_selinux_status),
                                            content = getSELinuxStatus(),
                                            icon = Icons.Filled.Security,
                                        )
                                        isFirstExpandedItem = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextCard() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_next_kernelsu_repo)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = getCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri(url)
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_next_kernelsu),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_next_kernelsu_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun EXperimentalCard() {
    /*val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_experimental_kernelsu_repo)
    */

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = getCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                /*.clickable {
                    uriHandler.openUri(url)
                }
                */
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_experimental_kernelsu),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_experimental_kernelsu_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_experimental_kernelsu_body_point_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_experimental_kernelsu_body_point_2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_experimental_kernelsu_body_point_3),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Get seasonal icon based on current month
private fun getSeasonalIcon(): ImageVector {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.MONTH)) {
        Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Icons.Filled.AcUnit // Winter
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Icons.Filled.Spa // Spring
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Icons.Filled.WbSunny // Summer
        else -> Icons.Filled.Forest // Fall
    }
}

// Get icon based on type and season
@Composable
private fun getIcon(iconType: String): Any {
    return when (iconType) {
        "OFF" -> Icons.Filled.VisibilityOff
        "SEASONAL" -> getSeasonalIcon()
        "WINTER" -> Icons.Filled.AcUnit
        "SPRING" -> Icons.Filled.Spa
        "SUMMER" -> Icons.Filled.WbSunny
        "FALL" -> Icons.Filled.Forest
        "KSU_NEXT" -> painterResource(R.drawable.ic_ksu_next)
        "CANNABIS" -> painterResource(R.drawable.ic_cannabis)
        else -> getSeasonalIcon()
    }
}

@Composable
fun IssueReportCard() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val selectedIconType = prefs.getString("selected_icon_type", "SEASONAL") ?: "SEASONAL"
    
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = getCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Help icon
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = "Help Card Icon",
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body_2),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
