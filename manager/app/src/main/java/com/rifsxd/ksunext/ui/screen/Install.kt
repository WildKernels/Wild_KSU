package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.*
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.BlurDialog
import com.rifsxd.ksunext.ui.component.DialogHandle
import com.rifsxd.ksunext.ui.component.SelectionDialog
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.util.*
import java.util.Locale

/**
 * @author weishu
 * @date 2024/3/12.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InstallScreen(navigator: DestinationsNavigator) {

    var installMethod by remember {
        mutableStateOf<InstallMethod?>(null)
    }

    var lkmSelection by remember {
        mutableStateOf<LkmSelection>(LkmSelection.KmiNone)
    }

    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdbd by rememberSaveable { mutableStateOf(false) }
    var noInstall by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val onInstall = {
        installMethod?.let { method ->
            if (method is InstallMethod.AnyKernel) {
                method.uri?.let {
                    navigator.navigate(
                        FlashScreenDestination(FlashIt.FlashAnyKernel(it))
                    )
                }
                return@let
            }

            if (method is InstallMethod.AnyKernelMagiskBoot) {
                navigator.navigate(
                    FlashScreenDestination(FlashIt.FlashAnyKernelMagiskBoot(method.zipUri, method.targetBootUri))
                )
                return@let
            }

            if (method is InstallMethod.UninstallLkm) {
                navigator.navigate(
                    FlashScreenDestination(FlashIt.FlashRestore)
                )
                return@let
            }

            val flashIt = FlashIt.FlashBoot(
                boot = if (method is InstallMethod.SelectFile) method.uri else null,
                lkm = lkmSelection,
                ota = method is InstallMethod.DirectInstallToInactiveSlot,
                allowShell = allowShell,
                enableAdbd = enableAdbd,
                noInstall = noInstall
            )
            navigator.navigate(FlashScreenDestination(flashIt))
        }
    }

    val currentKmi by produceState(initialValue = "") { value = getCurrentKmi() }

    val selectKmiDialog = rememberSelectKmiDialog { kmi ->
        kmi?.let {
            lkmSelection = LkmSelection.KmiString(it)
            onInstall()
        }
    }

    val onClickNext = {
        when (installMethod) {
            is InstallMethod.AnyKernel,
            is InstallMethod.UninstallLkm -> {
                onInstall()
            }

            else -> {
                if (!noInstall && lkmSelection == LkmSelection.KmiNone && currentKmi.isBlank()) {
                    // no lkm file selected and cannot get current kmi
                    selectKmiDialog.show()
                } else {
                    onInstall()
                }
            }
        }
    }

    val selectLkmLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    lkmSelection = LkmSelection.LkmUri(uri)
                }
            }
        }

    val onLkmUpload = {
        selectLkmLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
        })
    }

    val kernelVersion = getKernelVersion()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                onBack = dropUnlessResumed { navigator.popBackStack() },
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
        ) {
            var selectedCategory by rememberSaveable { mutableStateOf(InstallCategory.LKM) }

            val onLkmSelect = if (kernelVersion.isGKI()) onLkmUpload else null

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = selectedCategory.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        InstallCategory.values().forEach { category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = {
                                    if (selectedCategory != category) {
                                        selectedCategory = category
                                        installMethod = null
                                    }
                                },
                                text = {
                                    Text(
                                        text = when (category) {
                                            InstallCategory.LKM -> stringResource(R.string.install_category_lkm)
                                            InstallCategory.GKI -> stringResource(R.string.install_category_gki)
                                        },
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    AnimatedContent(
                        targetState = selectedCategory,
                        label = "InstallCategory",
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(tween(200)) { width -> direction * width } + fadeIn(tween(200)))
                                .togetherWith(slideOutHorizontally(tween(200)) { width -> -direction * width } + fadeOut(tween(200)))
                                .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(200) }))
                        }
                    ) { category ->
                        Column {
                            when (category) {
                                InstallCategory.LKM -> {
                                    SelectInstallMethod(
                                        currentMethod = installMethod,
                                        lkmSelection = lkmSelection,
                                        onLkmSelect = onLkmSelect
                                    ) { method ->
                                        installMethod = method
                                    }

                                    if (onLkmSelect != null) {
                                        HorizontalDivider()
                                        InstallItem(
                                            headline = stringResource(id = R.string.select_custom_lkm),
                                            supportingContent = when (val s = lkmSelection) {
                                                is LkmSelection.LkmUri -> s.uri.lastPathSegment
                                                is LkmSelection.KmiString -> s.value
                                                else -> null
                                            },
                                            leadingContent = {
                                                Icon(
                                                    imageVector = Icons.Filled.FileUpload,
                                                    contentDescription = stringResource(id = R.string.select_custom_lkm)
                                                )
                                            },
                                            onClick = onLkmSelect
                                        )
                                        HorizontalDivider()
                                    }

                                    SelectInstallOptions(
                                        allowShell, { allowShell = it },
                                        enableAdbd, { enableAdbd = it },
                                        noInstall, { noInstall = it }
                                    )
                                }

                                InstallCategory.GKI -> {
                                    SelectGkiInstallMethod(
                                        currentMethod = installMethod,
                                        onMethodSelected = { method ->
                                            installMethod = method
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                (lkmSelection as? LkmSelection.LkmUri)?.let {
                    Text(
                        stringResource(
                            id = R.string.selected_lkm,
                            it.uri.lastPathSegment ?: "(file)"
                        )
                    )
                }
                AnimatedVisibility(
                    visible = installMethod != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onClickNext()
                        }) {
                        Text(
                            stringResource(id = R.string.install_next),
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallItem(
    headline: String,
    supportingContent: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    role: Role? = null,
    content: @Composable (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                when (role) {
                    Role.RadioButton -> Modifier.selectable(
                        selected = selected,
                        onClick = onClick,
                        role = role
                    )
                    Role.Switch -> Modifier.toggleable(
                        value = selected,
                        onValueChange = { onClick() },
                        role = role
                    )
                    else -> Modifier.clickable(onClick = onClick, role = role)
                }
            } else Modifier
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                leadingContent()
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge
            )
            if (supportingContent != null) {
                Text(
                    text = supportingContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (content != null) {
                content()
            }
        }

        if (trailingContent != null) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun SelectInstallOptions(
    allowShell: Boolean,
    onAllowShellChange: (Boolean) -> Unit,
    enableAdbd: Boolean,
    onEnableAdbdChange: (Boolean) -> Unit,
    noInstall: Boolean,
    onNoInstallChange: (Boolean) -> Unit
) {
    Column {
        // Allow Shell
        InstallItem(
            headline = stringResource(R.string.install_option_allow_shell),
            supportingContent = stringResource(R.string.install_option_allow_shell_summary),
            trailingContent = {
                Switch(checked = allowShell, onCheckedChange = null)
            },
            onClick = { onAllowShellChange(!allowShell) },
            selected = allowShell,
            role = Role.Switch
        )

        // Enable Adbd
        InstallItem(
            headline = stringResource(R.string.install_option_enable_adbd),
            supportingContent = stringResource(R.string.install_option_enable_adbd_summary),
            trailingContent = {
                Switch(checked = enableAdbd, onCheckedChange = null)
            },
            onClick = { onEnableAdbdChange(!enableAdbd) },
            selected = enableAdbd,
            role = Role.Switch
        )

        // No Install
        InstallItem(
            headline = stringResource(R.string.install_option_no_install),
            supportingContent = stringResource(R.string.install_option_no_install_summary),
            trailingContent = {
                Switch(checked = noInstall, onCheckedChange = null)
            },
            onClick = { onNoInstallChange(!noInstall) },
            selected = noInstall,
            role = Role.Switch
        )
    }
}

enum class InstallCategory {
    LKM,
    GKI
}

sealed class InstallMethod {
    data class SelectFile(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.select_file,
        override val summary: String?
    ) : InstallMethod()

    data class AnyKernel(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.anykernel_install,
        override val summary: String? = null
    ) : InstallMethod()

    data class AnyKernelMagiskBoot(
        val zipUri: Uri,
        val targetBootUri: Uri,
        @param:StringRes override val label: Int = R.string.anykernel_magiskboot,
        override val summary: String? = null
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    data object UninstallLkm : InstallMethod() {
        override val label: Int
            get() = R.string.uninstall_lkm
    }

    abstract val label: Int
    open val summary: String? = null
}

@Composable
private fun SelectGkiInstallMethod(
    currentMethod: InstallMethod?,
    onMethodSelected: (InstallMethod) -> Unit
) {
    val context = LocalContext.current
    
    val selectAnyKernelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.AnyKernel(uri)
                onMethodSelected(option)
            }
        }
    }
    
    var tempMagiskBootZipUri by remember { mutableStateOf<Uri?>(null) }

    val selectAnyKernelMagiskBootTargetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { targetUri ->
                tempMagiskBootZipUri?.let { zipUri ->
                    val option = InstallMethod.AnyKernelMagiskBoot(zipUri, targetUri)
                    onMethodSelected(option)
                }
            }
        }
    }

    val selectAnyKernelMagiskBootZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                tempMagiskBootZipUri = uri
                android.widget.Toast.makeText(context, "Please select target boot image", android.widget.Toast.LENGTH_LONG).show()
                selectAnyKernelMagiskBootTargetLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/x-img"))
                    addCategory(Intent.CATEGORY_OPENABLE)
                })
            }
        }
    }


    Column {
        // AnyKernel3 Group
        if (rootAvailable()) {
            val method = InstallMethod.AnyKernel()
            val selected = currentMethod is InstallMethod.AnyKernel

            val uriName = if (selected && currentMethod is InstallMethod.AnyKernel) {
                currentMethod.uri?.lastPathSegment
            } else null

            InstallItem(
                headline = stringResource(method.label),
                supportingContent = uriName,
                leadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                },
                onClick = {
                    android.widget.Toast.makeText(context, "Please select AnyKernel3 zip", android.widget.Toast.LENGTH_SHORT).show()
                    selectAnyKernelLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                        addCategory(Intent.CATEGORY_OPENABLE)
                    })
                },
                selected = selected,
                role = Role.RadioButton
            )
        }

        val magiskBootMethod = InstallMethod.AnyKernelMagiskBoot(Uri.EMPTY, Uri.EMPTY)
        val magiskBootSelected = currentMethod is InstallMethod.AnyKernelMagiskBoot

        val magiskBootSummary = stringResource(R.string.anykernel_magiskboot_desc)
        val finalMagiskBootSummary = if (magiskBootSelected && currentMethod is InstallMethod.AnyKernelMagiskBoot) {
            val zipName = currentMethod.zipUri.lastPathSegment ?: "Not selected"
            val bootName = currentMethod.targetBootUri.lastPathSegment ?: "Not selected"
            buildString {
                append("1. Select AnyKernel3.zip\n")
                append("$zipName\n")
                append("2. Select boot.img\n")
                append("$bootName")
            }
        } else {
            magiskBootSummary
        }

        InstallItem(
            headline = stringResource(magiskBootMethod.label),
            supportingContent = finalMagiskBootSummary,
            leadingContent = {
                RadioButton(
                    selected = magiskBootSelected,
                    onClick = null
                )
            },
            onClick = {
                android.widget.Toast.makeText(context, "Please select AnyKernel3 zip", android.widget.Toast.LENGTH_SHORT).show()
                selectAnyKernelMagiskBootZipLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    addCategory(Intent.CATEGORY_OPENABLE)
                })
            },
            selected = magiskBootSelected,
            role = Role.RadioButton
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.install_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

private interface SelectKmiDialogHandle {
    fun show()
}

@Composable
private fun rememberSelectKmiDialog(onSelected: (String?) -> Unit): SelectKmiDialogHandle {
    var showDialog by remember { mutableStateOf(false) }
    
    val kmis by produceState<List<String>>(initialValue = emptyList()) {
        value = getSupportedKmis()
    }

    if (showDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_kmi),
            options = kmis.map { it to it },
            selectedOption = "",
            onOptionSelected = {
                onSelected(it)
                showDialog = false
            },
            onDismissRequest = {
                showDialog = false
            }
        )
    }

    return remember {
        object : SelectKmiDialogHandle {
            override fun show() {
                showDialog = true
            }
        }
    }
}



@Composable
private fun SelectInstallMethod(
    currentMethod: InstallMethod?,
    lkmSelection: LkmSelection,
    onLkmSelect: (() -> Unit)?,
    onSelected: (InstallMethod) -> Unit = {}
) {
    val rootAvailable = rootAvailable()
    val isAbDevice = produceState(initialValue = false) {
        value = isAbDevice()
    }.value
    val kernelVersion = getKernelVersion()
    val selectFileTip = stringResource(
        id = R.string.select_file_tip,
        if (kernelVersion.isKernel510())
            "boot"
        else
            "init_boot/vendor_boot"
    )
    val radioOptions = mutableListOf<InstallMethod>()

    radioOptions.add(InstallMethod.SelectFile(summary = selectFileTip))

    if (rootAvailable) {
        if (kernelVersion.isGKI()) {
            radioOptions.add(InstallMethod.DirectInstall)
            if (isAbDevice) {
                radioOptions.add(InstallMethod.DirectInstallToInactiveSlot)
            }
            radioOptions.add(InstallMethod.UninstallLkm)
        }
    }

    val context = LocalContext.current
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.SelectFile(uri, summary = selectFileTip)
                onSelected(option)
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(onConfirm = {
        onSelected(InstallMethod.DirectInstallToInactiveSlot)
    }, onDismiss = null)
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->

        when (option) {
            is InstallMethod.SelectFile -> {
                android.widget.Toast.makeText(context, "Please select image file", android.widget.Toast.LENGTH_SHORT).show()
                selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                })
            }

            is InstallMethod.DirectInstall, InstallMethod.UninstallLkm -> {
                onSelected(option)
            }

            is InstallMethod.DirectInstallToInactiveSlot -> {
                confirmDialog.showConfirm(dialogTitle, dialogContent)
            }

            else -> {}
        }
    }

    Column {
        radioOptions.forEach { option ->
            val selected = when (option) {
                is InstallMethod.SelectFile -> currentMethod is InstallMethod.SelectFile
                else -> option == currentMethod
            }

            val summary = if (selected && option is InstallMethod.SelectFile) {
                val uriName = (currentMethod as? InstallMethod.SelectFile)?.uri?.lastPathSegment
                if (uriName != null) {
                    "${option.summary}\n$uriName"
                } else {
                    option.summary
                }
            } else {
                option.summary
            }

            InstallItem(
                headline = stringResource(id = option.label),
                supportingContent = summary,
                leadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                },
                onClick = { onClick(option) },
                selected = selected,
                role = Role.RadioButton
            )
        }
    }
}


