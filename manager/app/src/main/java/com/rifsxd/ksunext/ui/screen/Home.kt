package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Build
import android.os.PowerManager
import android.system.Os
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.rifsxd.ksunext.ui.webui.WebUIActivity
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.*
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.theme.ORANGE
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.util.restartActivity
import com.rifsxd.ksunext.ui.util.module.LatestVersionInfo
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isManager = Natives.isManager
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()
    val ksuVersion = if (isManager) Natives.version else null
    val ksuVersionTag = if (isManager) Natives.getVersionTag() else null

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                kernelVersion,
                ksuVersion,
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination)
                },
                onInstallClick = {
                    navigator.navigate(InstallScreenDestination)
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lkmMode = ksuVersion?.let {
                if (kernelVersion.isGKI()) Natives.isLkmMode else null
            }

            StatusCard(kernelVersion, ksuVersion, lkmMode, ksuVersionTag = ksuVersionTag) {
                navigator.navigate(InstallScreenDestination)
            }

            if (fullFeatured) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperuserCard(onClick = { navigator.navigate(SuperUserScreenDestination) })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleCard(onClick = { navigator.navigate(ModuleScreenDestination) })
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
                    stringResource(id = R.string.grant_root_failed),
                    onClick = {
                        restartActivity(context)
                    }
                )
            }

            val checkUpdate =
                LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCard()
            }

            InfoCard(autoExpand = developerOptionsEnabled)
            IssueReportCard()
            Spacer(Modifier)
        }
    }
}

@Composable
private fun SuperuserCard(onClick: (() -> Unit)? = null) {
    val count = getSuperuserCount()
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = cardAlpha),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = cardElevation,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
private fun ModuleCard(onClick: (() -> Unit)? = null) {
    val count = getModuleCount()
    val moduleViewModel: ModuleViewModel = viewModel()
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    val moduleUpdateCount = moduleViewModel.moduleList.count {
        moduleViewModel.checkUpdate(it).first.isNotEmpty()
    }

    // State machine: 0 = nothing, 1 = show "+ Update!", 2 = show "+ X"
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(moduleUpdateCount) {
        if (moduleUpdateCount > 0) {
            step = 1
            delay(1200) // show "+ Update!" for a moment
            step = 2
        } else {
            step = 0
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = cardAlpha),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = cardElevation,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_module_count_singular)
                    } else {
                        stringResource(R.string.home_module_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (moduleUpdateCount > 0) {
                        Spacer(Modifier.width(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Keep the "|" static
                            Text(
                                text = "|",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.width(4.dp))

                            // Animate only the right-side text
                            AnimatedContent(
                                targetState = step,
                                transitionSpec = {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                },
                                label = "UpdateAnimation"
                            ) { target ->
                                when (target) {
                                    1 -> Text(
                                        text = "Update!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ORANGE
                                    )
                                    2 -> Text(
                                        text = buildAnnotatedString {
                                            append(moduleUpdateCount.toString())
                                            withStyle(SpanStyle(color = ORANGE)) {
                                                append("*")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
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
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog
    val newVersionTag = newVersion.versionTag

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = cardAlpha),
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            elevation = cardElevation,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
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
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Update,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 20.dp)
                )
                Text(
                    text = if (!newVersionTag.isNullOrEmpty()) {
                        stringResource(id = R.string.new_version_available, newVersionTag, newVersionCode)
                    } else {
                        stringResource(id = R.string.new_version_available, "", newVersionCode)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(
            text = stringResource(id),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }, onClick = {
        reboot(reason)
    })
}

// @Composable
// fun getSeasonalIcon(): ImageVector {
//     val month = Calendar.getInstance().get(Calendar.MONTH) // 0-11 for January-December
//     return when (month) {
//         Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Icons.Filled.AcUnit // Winter
//         Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Icons.Filled.Spa // Spring
//         Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Icons.Filled.WbSunny // Summer
//         Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Icons.Filled.Forest // Fall
//         else -> Icons.Filled.Whatshot // Fallback icon
//     }
// }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    onInstallClick: () -> Unit,
    onSettingsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSpinning by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(
            durationMillis = 1400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        finishedListener = {
            isSpinning = false
        }
    )

    LaunchedEffect(Unit) {
        isSpinning = true
        rotationTarget += 360f * 6
    }

    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isSpinning) {
                        isSpinning = true
                        rotationTarget += 360f * 6
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cannabis),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        },
        actions = {
            if (ksuVersion != null) {
                // Check for ksu_toolkit module and if it has a WebUI
                val moduleViewModel: ModuleViewModel = viewModel()
                val toolkitModule = moduleViewModel.moduleList.find { it.id == "ksu_toolkit" }
                
                if (toolkitModule != null && toolkitModule.hasWebUi) {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        webUILauncher.launch(
                            Intent(context, WebUIActivity::class.java)
                                .setData("kernelsu://webui/${toolkitModule.id}".toUri())
                                .putExtra("id", toolkitModule.id)
                                .putExtra("name", toolkitModule.name)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Filled.HomeRepairService,
                            contentDescription = "Toolkit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onInstallClick) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = stringResource(id = R.string.install)
                    )
                }
            }

            if (ksuVersion != null) {
                var showDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    showDropdown = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(id = R.string.reboot)
                    )
                }

                if (showDropdown) {
                    val baseScheme = LocalBaseColorScheme.current
                    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
                    MaterialTheme(
                        colorScheme = baseScheme,
                        typography = MaterialTheme.typography,
                        shapes = MaterialTheme.shapes,
                    ) {
                        Dialog(
                            onDismissRequest = { showDropdown = false }
                        ) {
                            // Apply blur behind the popup window if transparency is active and supported
                            if (cardAlpha < 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val view = LocalView.current
                                val context = LocalContext.current
                                DisposableEffect(view) {
                                    val root = view.rootView
                                    val params = root.layoutParams as? WindowManager.LayoutParams
                                    if (params != null) {
                                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                                        params.blurBehindRadius = 60 // Blur radius in pixels
                                        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                        wm.updateViewLayout(root, params)
                                    }
                                    onDispose {}
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = baseScheme.surfaceContainer.copy(alpha = cardAlpha),
                                ),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = if (cardAlpha < 1f) 0.dp else 6.dp
                                )
                            ) {
                                Box {
                                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        RebootDropdownItem(id = R.string.reboot)

                                        val pm =
                                            LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                                        @Suppress("DEPRECATION")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                            RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                                        }
                                        RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                                        RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                                        RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                                        RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(android.R.string.cancel),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = { showDropdown = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!LocalEnableBottomBar.current) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}


@Composable
private fun StatusCard(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?,
    moduleUpdateCount: Int = 0,
    ksuVersionTag: String? = null,
    onClickInstall: () -> Unit = {}
) {
    val context = LocalContext.current
    var tapCount by remember { mutableStateOf(0) }
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    val baseContainerColor =
        if (ksuVersion != null) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    val baseContentColor =
        if (ksuVersion != null) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = baseContainerColor.copy(alpha = cardAlpha),
            contentColor = baseContentColor,
        ),
        elevation = cardElevation,
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
                        } else {
                            onClickInstall()
                        }
                    } else if (ksuVersion == null) {
                        onClickInstall()
                    }
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ksuVersion != null -> {
                    val workingMode = if (lkmMode == true || lkmMode == false) {
                        val mode = if (lkmMode == true) "LKM" else "BUILT-IN"
                        "$mode (" + kernelVersion.getKernelType() + ")"
                    } else kernelVersion.getKernelType()

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

                        val versionText = if (!ksuVersionTag.isNullOrEmpty()) {
                            stringResource(id = R.string.home_working_version, ksuVersionTag, ksuVersion ?: 0)
                        } else {
                            stringResource(id = R.string.home_working_version, "v0.0.0", ksuVersion ?: 0)
                        }
                        Text(
                            text = versionText,
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
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = cardAlpha),
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        elevation = cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.padding(end = 20.dp)
            )
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoCard(autoExpand: Boolean = false) {
    val context = LocalContext.current
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val susfsVersion = if (isManager) getSuSFSVersion() else null
    val bbgVersion = if (isManager) getBBGVersion() else null

    var expanded by rememberSaveable { mutableStateOf(false) }

    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    LaunchedEffect(autoExpand) {
        if (autoExpand) {
            expanded = true
        }
    }   

    ElevatedCard(elevation = cardElevation) {
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

            Column {
                val managerVersion = getManagerVersion(context)
                InfoCardItem(
                    label = stringResource(R.string.home_manager_version),
                    content = if (
                        developerOptionsEnabled
                    ) {
                        "${managerVersion.first} (${managerVersion.second}) | UID: ${Natives.getManagerAppid()}"
                    } else {
                        "${managerVersion.first} (${managerVersion.second})"
                    },
                    icon = Icons.Filled.AutoAwesomeMotion,
                )

                if (ksuVersion != null) {

                    val hookMode =
                        Natives.getHookMode()
                            .takeUnless { it.isNullOrBlank() }
                            ?: stringResource(R.string.unavailable)

                    Spacer(Modifier.height(16.dp))

                    InfoCardItem(
                        label   = stringResource(R.string.hook_mode),
                        content = hookMode,
                        icon    = Icons.Filled.Phishing,
                    )
                }

                if (ksuVersion != null) {
                    Spacer(Modifier.height(16.dp))
                    
                    val moduleViewModel: ModuleViewModel = viewModel()
                    val meta = moduleViewModel.moduleList.firstOrNull {
                        it.isMetaModule && it.enabled && !it.remove
                    }

                    val mountSystem = currentMountSystem()
                        .ifBlank { stringResource(R.string.unavailable) }

                    val content = listOfNotNull(
                        mountSystem,
                        meta?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.home_not_installed),
                        meta?.version?.takeIf { it.isNotBlank() }
                    ).joinToString(" | ")

                    InfoCardItem(
                        label = stringResource(R.string.home_mount_system),
                        content = content,
                        icon = Icons.Filled.SettingsSuggest
                    )

                    if (susfsVersion != null) {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_susfs_version),
                            content = "${stringResource(R.string.susfs_supported)} | $susfsVersion",
                            icon = painterResource(R.drawable.ic_sus),
                        )
                    }

                    if (bbgVersion != null) {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_bbg_version),
                            content = "${stringResource(R.string.bbg_supported)} | $bbgVersion",
                            icon = Icons.Filled.Security,
                        )
                    }

                    if (Natives.isZygiskEnabled()) {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.zygisk_status),
                            content = "${stringResource(R.string.enabled)} | ${getZygiskImplementation("name")} | ${getZygiskImplementation("version")}",
                            icon = Icons.Filled.Vaccines
                        )
                    }
                }

                if (!expanded) {
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

                AnimatedVisibility(visible = expanded) {
                    val uname = Os.uname()
                    Column {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_kernel),
                            content = "${uname.release} (${uname.machine})",
                            icon = painterResource(R.drawable.ic_linux),
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_android),
                            content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
                            icon = Icons.Filled.Android,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_abi),
                            content = Build.SUPPORTED_ABIS.joinToString(", "),
                            icon = Icons.Filled.Memory,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_selinux_status),
                            content = getSELinuxStatus(),
                            icon = Icons.Filled.Security,
                        )
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
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    ElevatedCard(elevation = cardElevation) {

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

    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    ElevatedCard(elevation = cardElevation) {

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

@Composable
fun IssueReportCard() {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
    val cardElevation = if (cardAlpha < 1f) {
        CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.elevatedCardElevation()
    }

    ElevatedCard(elevation = cardElevation) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
