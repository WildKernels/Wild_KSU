package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.ConfirmResult
import com.rifsxd.ksunext.ui.component.KeyEventBlocker
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.theme.GREEN
import com.rifsxd.ksunext.ui.theme.ORANGE
import com.rifsxd.ksunext.ui.theme.RED
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.rifsxd.ksunext.ui.viewmodel.FlashViewModel
import com.rifsxd.ksunext.ui.viewmodel.FlashingStatus
import com.rifsxd.ksunext.ui.LocalFlashViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Lets you flash modules sequentially when mutiple zipUris are selected
fun flashModulesSequentially(
    uris: List<Uri>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    for (uri in uris) {
        flashModule(uri, onStdout, onStderr).apply {
            if (code != 0) {
                return FlashResult(code, err, showReboot)
            }
        }
    }
    return FlashResult(0, "", true)
}

/**
 * @author weishu
 * @date 2023/1/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>
fun FlashScreen(
    navigator: DestinationsNavigator,
    flashIt: FlashIt,
    finishIntent: Boolean = false
) {

    var text by rememberSaveable { mutableStateOf("") }
    var tempText: String
    val logContent = rememberSaveable { StringBuilder() }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }

    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val flashViewModel = LocalFlashViewModel.current
    val flashing = flashViewModel.flashingStatus

    val context = LocalContext.current

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    val activity = context.findActivity()

    // Reset flashing status when screen is entered
    LaunchedEffect(Unit) {
        flashViewModel.resetFlashingStatus()
    }

    val view = LocalView.current
    DisposableEffect(flashing) {
        view.keepScreenOn = flashing == FlashingStatus.FLASHING
        onDispose {
            view.keepScreenOn = false
        }
    }

    BackHandler(enabled = flashing == FlashingStatus.FLASHING) {
        // Disable back button if flashing is running
    }

    BackHandler(enabled = flashing != FlashingStatus.FLASHING) {
        navigator.popBackStack()
        if (finishIntent) activity?.finish()
    }

    val confirmDialog = rememberConfirmDialog()
    var confirmed by rememberSaveable { mutableStateOf(flashIt !is FlashIt.FlashModules) }
    var pendingFlashIt by rememberSaveable { mutableStateOf<FlashIt?>(null) }
    var hasFlashed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(flashIt) {
        if (flashIt is FlashIt.FlashModules && !confirmed) {
            val uris = flashIt.uris
            val moduleNames =
                uris.mapIndexed { index, uri -> "\n${index + 1}. ${uri.getFileName(context)}" }
                    .joinToString("")
            val confirmContent =
                context.getString(R.string.module_install_prompt_with_name, moduleNames)
            val confirmTitle = context.getString(R.string.module)
            val result = confirmDialog.awaitConfirm(
                title = confirmTitle,
                content = confirmContent,
                markdown = true
            )
            if (result == ConfirmResult.Confirmed) {
                confirmed = true
                pendingFlashIt = flashIt
            } else {
                // User cancelled, go back
                navigator.popBackStack()
                if (finishIntent) activity?.finish()
            }
        } else {
            confirmed = true
            pendingFlashIt = flashIt
        }
    }

    LaunchedEffect(confirmed, pendingFlashIt) {
        if (!confirmed || pendingFlashIt == null || text.isNotEmpty() || hasFlashed) return@LaunchedEffect
        hasFlashed = true
        // Set status to FLASHING when operation begins
        flashViewModel.updateFlashingStatus(FlashingStatus.FLASHING)
        withContext(Dispatchers.IO) {
            flashIt(pendingFlashIt!!, onStdout = {
                tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    text = tempText.substring(6)
                } else {
                    text += tempText
                }
                logContent.append(it).append("\n")
            }, onStderr = {
                logContent.append(it).append("\n")
            }).apply {
                if (code != 0) {
                    text += "Error code: $code.\n $err Please save and check the log.\n"
                }
                if (showReboot) {
                    text += "\n\n\n"
                    showFloatAction = true
                }
                flashViewModel.updateFlashingStatus(if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (flashIt is FlashIt.FlashModules && (flashing == FlashingStatus.SUCCESS)) {
                // Reboot button for modules flashing
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                reboot()
                            }
                        }
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.reboot),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stringResource(R.string.reboot))
                }
            }

            if (flashIt is FlashIt.FlashModules && (flashing == FlashingStatus.FAILED)) {
                // Close button for modules flashing
                FilledTonalButton(
                    onClick = {
                        navigator.popBackStack()
                        if (finishIntent) activity?.finish()
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stringResource(R.string.close))
                }
            }

            if (flashIt is FlashIt.FlashBoot && (flashing == FlashingStatus.SUCCESS || flashing == FlashingStatus.FAILED)) {
                val isLocalPatch = flashIt.boot != null && !flashIt.ota
                val isDirectOrOta = flashIt.boot == null || flashIt.ota

                if (flashing == FlashingStatus.FAILED) {
                    // Always show close on failure
                    FilledTonalButton(
                        onClick = {
                            navigator.popBackStack()
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = stringResource(R.string.close))
                    }
                } else if (flashing == FlashingStatus.SUCCESS) {
                    if (isLocalPatch) {
                        // Local patching: show only Close
                        FilledTonalButton(
                            onClick = {
                                navigator.popBackStack()
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.close))
                        }
                    } else if (isDirectOrOta) {
                        // Direct install or OTA inactive slot: show only Reboot
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        reboot()
                                    }
                                }
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.reboot),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.reboot))
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom),
        snackbarHost = { SnackbarHost(hostState = snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = if (developerOptionsEnabled) logContent.toString() else text,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

fun Uri.getFileName(context: Context): String {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(this, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            it.getString(nameIndex)
        } else {
            this.lastPathSegment ?: "unknown.zip"
        }
    } ?: (this.lastPathSegment ?: "unknown.zip")
}

@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashBoot(val boot: Uri? = null, val lkm: LkmSelection, val ota: Boolean) :
        FlashIt()

    data class FlashModules(val uris: List<Uri>) : FlashIt()

    data object FlashRestore : FlashIt()

    data object FlashUninstall : FlashIt()
}

fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return when (flashIt) {
        is FlashIt.FlashBoot -> installBoot(
            flashIt.boot,
            flashIt.lkm,
            flashIt.ota,
            onStdout,
            onStderr
        )

        is FlashIt.FlashModules -> {
            flashModulesSequentially(flashIt.uris, onStdout, onStderr)
        }

        FlashIt.FlashRestore -> restoreBoot(onStdout, onStderr)

        FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
    }
}



@Preview
@Composable
fun InstallPreview() {
    InstallScreen(EmptyDestinationsNavigator)
}
