package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.IconSource
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.BuildConfig
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.component.*
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author weishu
 * @date 2023/1/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    var isGlobalNamespaceEnabled by rememberSaveable { mutableStateOf(false) }
    isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()

    val isManager = Natives.becomeManager(context.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    val aboutDialog = rememberCustomDialog {
        AboutDialog(it)
    }
    val loadingDialog = rememberLoadingDialog()
    val shrinkDialog = rememberConfirmDialog()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            loadingDialog.show()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                getBugreportFile(context).inputStream().use {
                    it.copyTo(output)
                }
            }
            loadingDialog.hide()
            snackBarHost.showSnackbar(context.getString(R.string.log_saved))
        }
    }

    var umountChecked by rememberSaveable {
        mutableStateOf(Natives.isDefaultUmountModules())
    }
    
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val useOverlayFs by observePreferenceAsState(prefs, "use_overlay_fs", false)
    var showRebootDialog by remember { mutableStateOf(false) }
    val isOverlayAvailable = overlayFsAvailable()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(CardConstants.CARD_PADDING_MEDIUM),
        verticalArrangement = Arrangement.spacedBy(CardConstants.CARD_PADDING_MEDIUM)
    ) {
        
        // First Card: Core Settings
        item {
            StandardCard {
                if (ksuVersion != null) {
                    CardSwitchContent(
                        icon = Icons.Filled.FolderDelete,
                        title = stringResource(id = R.string.settings_umount_modules_default),
                        subtitle = stringResource(id = R.string.settings_umount_modules_default_summary),
                        checked = umountChecked
                    ) {
                        if (Natives.setDefaultUmountModules(it)) {
                            umountChecked = it
                        }
                    }
                }

                if (ksuVersion != null && isOverlayAvailable) {
                    CardItemSpacer()
                    
                    CardSwitchContent(
                        icon = Icons.Filled.Build,
                        title = stringResource(id = R.string.use_overlay_fs),
                        subtitle = stringResource(id = R.string.use_overlay_fs_summary),
                        checked = useOverlayFs
                    ) {
                        prefs.edit().putBoolean("use_overlay_fs", it).apply()
                        if (it) {
                            moduleBackup()
                            updateMountSystemFile(true)
                        } else {
                            moduleMigration()
                            updateMountSystemFile(false)
                        }
                        if (isManager) install()
                        showRebootDialog = true
                    }
                }

                if (ksuVersion != null) {
                    CardItemSpacer()
                    
                    if (Natives.version >= Natives.MINIMAL_SUPPORTED_SU_COMPAT) {
                        var isSuDisabled by rememberSaveable {
                            mutableStateOf(!Natives.isSuEnabled())
                        }
                        CardSwitchContent(
                            icon = Icons.Filled.RemoveModerator,
                            title = stringResource(id = R.string.settings_disable_su),
                            subtitle = stringResource(id = R.string.settings_disable_su_summary),
                            checked = isSuDisabled
                        ) { checked ->
                            val shouldEnable = !checked
                            if (Natives.setSuEnabled(shouldEnable)) {
                                isSuDisabled = !shouldEnable
                            }
                        }
                        
                        CardItemSpacer()
                    }
                    
                    CardSwitchContent(
                        icon = Icons.Filled.Engineering,
                        title = stringResource(id = R.string.settings_global_namespace_mode),
                        subtitle = stringResource(id = R.string.settings_global_namespace_mode_summary),
                        checked = isGlobalNamespaceEnabled,
                        onCheckedChange = {
                            setGlobalNamespaceEnabled(
                                if (isGlobalNamespaceEnabled) {
                                    "0"
                                } else {
                                    "1"
                                }
                            )
                            isGlobalNamespaceEnabled = it
                        }
                    )
                }

                val suSFS = getSuSFS()
                val isSUS_SU = hasSuSFs_SUS_SU() == "Supported"
                if (suSFS == "Supported") {
                    if (isSUS_SU) {
                        CardItemSpacer()
                        
                        val isEnabled by observePreferenceAsState(prefs, "enable_sus_su", false)

                        CardSwitchContent(
                            icon = Icons.Filled.VisibilityOff,
                            title = stringResource(id = R.string.settings_susfs_toggle),
                            subtitle = stringResource(id = R.string.settings_susfs_toggle_summary),
                            checked = isEnabled
                        ) {
                            if (it) {
                                susfsSUS_SU_2()
                            } else {
                                susfsSUS_SU_0()
                            }
                            prefs.edit().putBoolean("enable_sus_su", it).apply()
                        }
                    }
                }

                if (suSFS == "Supported" && isSUS_SU && isOverlayAvailable && useOverlayFs) {
                    CardItemSpacer()
                }

                if (isOverlayAvailable && useOverlayFs) {
                    val shrink = stringResource(id = R.string.shrink_sparse_image)
                    val shrinkMessage = stringResource(id = R.string.shrink_sparse_image_message)
                    CardRowContent(
                        text = shrink,
                        icon = Icons.Filled.Compress,
                        modifier = Modifier.clickable(
                            onClick = {
                                scope.launch {
                                    val result = shrinkDialog.awaitConfirm(title = shrink, content = shrinkMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            shrinkModules()
                                        }
                                    }
                                }
                            },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        )
                    )
                }
                
                val lkmMode = Natives.version >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && Natives.isLkmMode
                if (isOverlayAvailable && useOverlayFs) {
                    if (lkmMode) {
                        CardItemSpacer()
                    }
                }
                if (lkmMode) {
                    UninstallItem(navigator) {
                        loadingDialog.withLoading(it)
                    }
                }
            }
        }
        
        // Second Card: App Settings
        item {
            StandardCard {
                val checkUpdate by observePreferenceAsState(prefs, "check_update", false)
                CardSwitchContent(
                    icon = Icons.Filled.Update,
                    title = stringResource(id = R.string.settings_check_update),
                    subtitle = stringResource(id = R.string.settings_check_update_summary),
                    checked = checkUpdate
                ) {
                    prefs.edit().putBoolean("check_update", it).apply()
                }

                CardItemSpacer()

                val customization = stringResource(id = R.string.customization)
                CardRowContent(
                    text = customization,
                    icon = Icons.Filled.Palette,
                    modifier = Modifier.clickable(
                        onClick = { navigator.navigate(CustomizationScreenDestination) },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                if (ksuVersion != null) {
                    val backupRestore = stringResource(id = R.string.backup_restore)
                    CardRowContent(
                        text = backupRestore,
                        icon = Icons.Filled.Backup,
                        modifier = Modifier.clickable(
                            onClick = { navigator.navigate(BackupRestoreScreenDestination) },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        )
                    )
                    
                    CardItemSpacer()
                    
                    val developer = stringResource(id = R.string.developer)
                    CardRowContent(
                        text = developer,
                        icon = Icons.Filled.DeveloperBoard,
                        modifier = Modifier.clickable(
                            onClick = { navigator.navigate(DeveloperScreenDestination) },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        )
                    )
                    
                    CardItemSpacer()
                }

                val exportLogsDialog = rememberCustomDialog { dismiss ->
                    ExportLogsDialog(
                        onSaveLog = {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                            val current = LocalDateTime.now().format(formatter)
                            exportBugreportLauncher.launch("KernelSU_Next_bugreport_${current}.tar.gz")
                            dismiss()
                        },
                        onShareLog = {
                            scope.launch {
                                val bugreport = loadingDialog.withLoading {
                                    withContext(Dispatchers.IO) {
                                        getBugreportFile(context)
                                    }
                                }

                                val uri: Uri =
                                    FileProvider.getUriForFile(
                                        context,
                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                        bugreport
                                    )

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    setDataAndType(uri, "application/gzip")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.send_log)
                                    )
                                )
                            }
                            dismiss()
                        },
                        onDismiss = dismiss
                    )
                }

                CardRowContent(
                    text = stringResource(id = R.string.export_log),
                    icon = Icons.Filled.BugReport,
                    modifier = Modifier.clickable(
                        onClick = { exportLogsDialog.show() },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                val about = stringResource(id = R.string.about)
                CardRowContent(
                    text = about,
                    icon = Icons.Filled.ContactPage,
                    modifier = Modifier.clickable(
                        onClick = { aboutDialog.show() },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )
            }
        }
    }
    
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text(
                text = stringResource(R.string.reboot_required),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            ) },
            text = { Text(stringResource(R.string.reboot_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRebootDialog = false
                    reboot()
                }) {
                    Text(stringResource(R.string.reboot))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}

@Composable
fun UninstallItem(
    navigator: DestinationsNavigator,
    withLoading: suspend (suspend () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uninstallConfirmDialog = rememberConfirmDialog()
    val showTodo = {
        Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
    }
    val uninstallDialog = rememberUninstallDialog { uninstallType ->
        scope.launch {
            val result = uninstallConfirmDialog.awaitConfirm(
                title = context.getString(uninstallType.title),
                content = context.getString(uninstallType.message)
            )
            if (result == ConfirmResult.Confirmed) {
                withLoading {
                    when (uninstallType) {
                        UninstallType.TEMPORARY -> showTodo()
                        UninstallType.PERMANENT -> navigator.navigate(
                            FlashScreenDestination(FlashIt.FlashUninstall)
                        )
                        UninstallType.RESTORE_STOCK_IMAGE -> navigator.navigate(
                            FlashScreenDestination(FlashIt.FlashRestore)
                        )
                        UninstallType.NONE -> Unit
                    }
                }
            }
        }
    }
    val uninstall = stringResource(id = R.string.settings_uninstall)
    ListItem(
        leadingContent = {
            Icon(
                Icons.Filled.Delete,
                uninstall
            )
        },
        headlineContent = { Text(
            text = uninstall,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        ) },
        modifier = Modifier.clickable(
            onClick = { uninstallDialog.show() },
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        )
    )
}

enum class UninstallType(val title: Int, val message: Int, val icon: ImageVector) {
    TEMPORARY(
        R.string.settings_uninstall_temporary,
        R.string.settings_uninstall_temporary_message,
        Icons.Filled.Delete
    ),
    PERMANENT(
        R.string.settings_uninstall_permanent,
        R.string.settings_uninstall_permanent_message,
        Icons.Filled.DeleteForever
    ),
    RESTORE_STOCK_IMAGE(
        R.string.settings_restore_stock_image,
        R.string.settings_restore_stock_image_message,
        Icons.AutoMirrored.Filled.Undo
    ),
    NONE(0, 0, Icons.Filled.Delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberUninstallDialog(onSelected: (UninstallType) -> Unit): DialogHandle {
    return rememberCustomDialog { dismiss ->
        val options = listOf(
            // UninstallType.TEMPORARY,
            UninstallType.PERMANENT,
            UninstallType.RESTORE_STOCK_IMAGE
        )
        val listOptions = options.map {
            ListOption(
                titleText = stringResource(it.title),
                subtitleText = if (it.message != 0) stringResource(it.message) else null,
                icon = IconSource(it.icon)
            )
        }

        var selection = UninstallType.NONE
        ListDialog(state = rememberUseCaseState(visible = true, onFinishedRequest = {
            if (selection != UninstallType.NONE) {
                onSelected(selection)
            }
        }, onCloseRequest = {
            dismiss()
        }), header = Header.Default(
            title = stringResource(R.string.settings_uninstall),
        ), selection = ListSelection.Single(
            showRadioButtons = false,
            options = listOptions,
        ) { index, _ ->
            selection = options[index]
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportLogsDialog(
    onSaveLog: () -> Unit,
    onShareLog: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.export_log),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose how you want to export the logs:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSaveLog() }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.save_log),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                onClick = { onShareLog() },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.send_log),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
private fun SettingsPreview() {
    SettingScreen(EmptyDestinationsNavigator)
}
