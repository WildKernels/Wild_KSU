package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
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

            if (method is InstallMethod.KpnSelectFile) {
                method.uri?.let {
                    navigator.navigate(
                        FlashScreenDestination(FlashIt.FlashKpn(it))
                    )
                }
                return@let
            }

            if (method is InstallMethod.KpnDirect) {
                navigator.navigate(
                    FlashScreenDestination(FlashIt.FlashKpn(null))
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
            is InstallMethod.AnyKernel -> {
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

            InstallCategoryCard(
                title = stringResource(R.string.install_category_lkm),
                selected = selectedCategory == InstallCategory.LKM,
                infoTitle = stringResource(R.string.lkm_mode_title),
                infoDesc = stringResource(R.string.lkm_mode_desc),
                onSelect = { selectedCategory = InstallCategory.LKM }
            ) {
                SelectInstallMethod(
                    lkmSelection = lkmSelection,
                    onLkmSelect = onLkmSelect
                ) { method ->
                    installMethod = method
                }

                HorizontalDivider()
                
                if (onLkmSelect != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLkmSelect)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = stringResource(id = R.string.select_custom_lkm)
                        )
                        Column(
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.select_custom_lkm),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            val currentLkmSelection = lkmSelection
                            val summary = when (currentLkmSelection) {
                                is LkmSelection.LkmUri -> currentLkmSelection.uri.lastPathSegment
                                is LkmSelection.KmiString -> currentLkmSelection.value
                                else -> null
                            }

                            if (summary != null) {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                SelectInstallOptions(
                    allowShell, { allowShell = it },
                    enableAdbd, { enableAdbd = it },
                    noInstall, { noInstall = it }
                )
            }

            InstallCategoryCard(
                title = stringResource(R.string.install_category_gki),
                selected = selectedCategory == InstallCategory.GKI,
                infoTitle = stringResource(R.string.gki_mode_title),
                infoDesc = stringResource(R.string.gki_mode_desc),
                onSelect = { selectedCategory = InstallCategory.GKI }
            ) {
                SelectGkiInstallMethod(installMethod) { method ->
                    installMethod = method
                }
            }

            InstallCategoryCard(
                title = stringResource(R.string.install_category_kpn),
                selected = selectedCategory == InstallCategory.KPN,
                infoTitle = stringResource(R.string.kpn_mode_title),
                infoDesc = stringResource(R.string.kpn_mode_desc),
                onSelect = { selectedCategory = InstallCategory.KPN }
            ) {
                SelectKpnInstallMethod(installMethod) { method ->
                    installMethod = method
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
                Button(modifier = Modifier.fillMaxWidth(),
                    enabled = installMethod != null,
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

@Composable
private fun SelectInstallOptions(
    allowShell: Boolean,
    onAllowShellChange: (Boolean) -> Unit,
    enableAdbd: Boolean,
    onEnableAdbdChange: (Boolean) -> Unit,
    noInstall: Boolean,
    onNoInstallChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Allow Shell
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = allowShell,
                    onValueChange = onAllowShellChange,
                    role = Role.Switch
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.install_option_allow_shell),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.install_option_allow_shell_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = allowShell, onCheckedChange = null)
        }

        // Enable Adbd
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = enableAdbd,
                    onValueChange = onEnableAdbdChange,
                    role = Role.Switch
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.install_option_enable_adbd),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.install_option_enable_adbd_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enableAdbd, onCheckedChange = null)
        }

        // No Install
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = noInstall,
                    onValueChange = onNoInstallChange,
                    role = Role.Checkbox
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.install_option_no_install),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.install_option_no_install_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = noInstall, onCheckedChange = null)
        }
    }
}

enum class InstallCategory {
    LKM,
    KPN,
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

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    data class KpnSelectFile(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.kpn_select_file,
        override val summary: String? = null
    ) : InstallMethod()

    data object KpnDirect : InstallMethod() {
        override val label: Int
            get() = R.string.kpn_direct_install
    }

    data object KpnPatchAndFlash : InstallMethod() {
        override val label: Int
            get() = R.string.kpn_patch_and_flash
    }

    abstract val label: Int
    open val summary: String? = null
}

@Composable
private fun SelectGkiInstallMethod(
    currentMethod: InstallMethod?,
    onMethodSelected: (InstallMethod) -> Unit
) {
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

    Column(modifier = Modifier.padding(16.dp)) {
        val method = InstallMethod.AnyKernel()
        val selected = currentMethod is InstallMethod.AnyKernel

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectAnyKernelLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                        addCategory(Intent.CATEGORY_OPENABLE)
                    })
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = {
                    selectAnyKernelLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                        addCategory(Intent.CATEGORY_OPENABLE)
                    })
                }
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = stringResource(method.label),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
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
                text = stringResource(R.string.install),
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
fun InstallCategoryCard(
    title: String,
    selected: Boolean,
    infoTitle: String? = null,
    infoDesc: String? = null,
    onSelect: () -> Unit,
    content: @Composable () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && infoTitle != null && infoDesc != null) {
        BlurDialog(
            onDismissRequest = { showInfo = false }
        ) {
            Text(
                text = infoTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = infoDesc,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSelect() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = onSelect)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    fontWeight = FontWeight.Bold
                )
                if (infoTitle != null && infoDesc != null) {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.issue_report_title)
                        )
                    }
                }
            }
            if (selected) {
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
private fun SelectInstallMethod(
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
        }
    }

    var selectedOption by remember { mutableStateOf<InstallMethod?>(null) }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.SelectFile(uri, summary = selectFileTip)
                selectedOption = option
                onSelected(option)
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(onConfirm = {
        selectedOption = InstallMethod.DirectInstallToInactiveSlot
        onSelected(InstallMethod.DirectInstallToInactiveSlot)
    }, onDismiss = null)
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->

        when (option) {
            is InstallMethod.SelectFile -> {
                selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                })
            }

            is InstallMethod.DirectInstall -> {
                selectedOption = option
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (option == selectedOption),
                        onClick = { onClick(option) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = null // null recommended for accessibility with selectable
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = option.label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (option.summary != null) {
                        Text(
                            text = option.summary!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectKpnInstallMethod(
    currentMethod: InstallMethod?,
    onMethodSelected: (InstallMethod) -> Unit
) {
    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.KpnSelectFile(uri)
                onMethodSelected(option)
            }
        }
    }

    val kpnOptions = listOf(
        InstallMethod.KpnSelectFile(summary = stringResource(R.string.kpn_select_file_desc)),
        InstallMethod.KpnDirect
    )

    Column(modifier = Modifier.padding(16.dp)) {
        kpnOptions.forEach { option ->
            val selected = if (option is InstallMethod.KpnSelectFile) {
                currentMethod is InstallMethod.KpnSelectFile
            } else {
                currentMethod is InstallMethod.KpnDirect
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = {
                            if (option is InstallMethod.KpnSelectFile) {
                                selectFileLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "application/octet-stream"
                                })
                            } else {
                                onMethodSelected(option)
                            }
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = stringResource(option.label),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val summary = if (option is InstallMethod.KpnDirect) {
                        stringResource(R.string.kpn_direct_install_desc)
                    } else {
                        option.summary
                    }

                    if (summary != null) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selected && currentMethod is InstallMethod.KpnSelectFile && currentMethod.uri != null) {
                        Text(
                            text = currentMethod.uri.lastPathSegment ?: "(file)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
