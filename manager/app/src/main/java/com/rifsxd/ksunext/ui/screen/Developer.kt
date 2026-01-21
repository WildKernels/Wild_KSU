package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import androidx.compose.material.icons.filled.Update
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.component.rememberLoadingDialog
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import com.rifsxd.ksunext.BuildConfig

/**
 * @author rifsxd
 * @date 2025/6/15.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DeveloperScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val loadingDialog = rememberLoadingDialog()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            // --- Update Manager Helper ---
            val installUpdate: (String) -> Unit = { branch ->
                scope.launch {
                    loadingDialog.withLoading {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val url = "https://nightly.link/WildKernels/Wild_KSU/workflows/build-manager/$branch/manager.zip"
                                val request = okhttp3.Request.Builder().url(url).build()

                                ksuApp.okhttpClient.newCall(request).execute().use { response ->
                                    if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

                                    val zipFile = File(context.cacheDir, "manager_ci.zip")
                                    response.body?.byteStream()?.use { input ->
                                        FileOutputStream(zipFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    // Unzip to find apk
                                    var apkFile: File? = null
                                    ZipInputStream(zipFile.inputStream()).use { zipInput ->
                                        var entry = zipInput.nextEntry
                                        while (entry != null) {
                                            if (entry.name.endsWith(".apk")) {
                                                apkFile = File(context.cacheDir, "manager_ci.apk")
                                                FileOutputStream(apkFile!!).use { output ->
                                                    zipInput.copyTo(output)
                                                }
                                                break
                                            }
                                            entry = zipInput.nextEntry
                                        }
                                    }

                                    zipFile.delete()

                                    if (apkFile != null) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                                            apkFile!!
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        throw Exception("No APK found in ZIP")
                                    }
                                }
                            }.onFailure {
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar("Update failed: ${it.message}")
                                }
                            }
                        }
                    }
                }
            }

            // --- Force Update (Stable) ---
            ListItem(
                headlineContent = {
                    Text(
                        text = "Update Manager (Stable)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Text("Download and install latest manager from Stable branch")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Update,
                        contentDescription = "Update Stable"
                    )
                },
                modifier = Modifier.clickable {
                    installUpdate("stable")
                }
            )

            // --- Force Update (Beta) ---
            ListItem(
                headlineContent = {
                    Text(
                        text = "Update Manager (Beta)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Text("Download and install latest manager from Beta branch")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Update,
                        contentDescription = "Update Beta"
                    )
                },
                modifier = Modifier.clickable {
                    installUpdate("beta")
                }
            )

            // --- Force Update (Canary) ---
            ListItem(
                headlineContent = {
                    Text(
                        text = "Update Manager (Canary)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Text("Download and install latest manager from Canary branch")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Update,
                        contentDescription = "Update Canary"
                    )
                },
                modifier = Modifier.clickable {
                    installUpdate("canary")
                }
            )

            // --- Developer Options Switch ---
            var developerOptionsEnabled by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_developer_options", false)
                )
            }
            if (ksuVersion != null) {
                SwitchItem(
                    icon = Icons.Filled.DeveloperMode,
                    title = stringResource(id = R.string.enable_developer_options),
                    summary = stringResource(id = R.string.enable_developer_options_summary),
                    checked = developerOptionsEnabled
                ) {
                    prefs.edit { putBoolean("enable_developer_options", it) }
                    developerOptionsEnabled = it
                }
            }

            var enableWebDebugging by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_web_debugging", false)
                )
            }
            if (ksuVersion != null) {
                SwitchItem(
                    enabled = developerOptionsEnabled,
                    icon = Icons.Filled.Web,
                    title = stringResource(id = R.string.enable_web_debugging),
                    summary = stringResource(id = R.string.enable_web_debugging_summary),
                    checked = enableWebDebugging
                ) {
                    prefs.edit { putBoolean("enable_web_debugging", it) }
                    enableWebDebugging = it
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
    TopAppBar(
        title = { Text(
                text = stringResource(R.string.developer),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) }, navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun DeveloperPreview() {
    DeveloperScreen(EmptyDestinationsNavigator)
}
