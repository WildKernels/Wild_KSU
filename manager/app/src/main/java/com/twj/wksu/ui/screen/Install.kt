package com.twj.wksu.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.twj.wksu.*
import com.twj.wksu.R
import com.twj.wksu.ui.util.*

/**
 * @author weishu
 * @date 2024/3/12.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InstallScreen(navigator: DestinationsNavigator) {
    var selectedOption by remember { mutableStateOf<InstallMethod?>(null) }
    val context = LocalContext.current

    val anyKernelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        navigator.navigate(FlashScreenDestination(FlashIt.FlashAnyKernel(uri)))
    }

    val onInstall = {
        when (selectedOption) {
            is InstallMethod.AnyKernel -> {
                anyKernelPicker.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    addCategory(Intent.CATEGORY_OPENABLE)
                })
            }

            else -> {
                // no-op
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed { navigator.popBackStack() },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectInstallMethod(selectedOption = selectedOption) {
                selectedOption = it
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedOption != null,
                onClick = onInstall
            ) {
                Text(
                    text = stringResource(id = R.string.install_next),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
        }
    }
}

sealed class InstallMethod {
    data object AnyKernel : InstallMethod() {
        @StringRes
        val label: Int = R.string.flash_anykernel
    }
}

@Composable
private fun SelectInstallMethod(
    selectedOption: InstallMethod?,
    onSelected: (InstallMethod) -> Unit
) {
    val options = listOf(InstallMethod.AnyKernel)

    Column {
        options.forEach { option ->
            val interactionSource = remember { MutableInteractionSource() }
            val selected = selectedOption == option

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selected,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.RadioButton,
                        onValueChange = {
                            onSelected(option)
                        }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(InstallMethod.AnyKernel.label),
                    style = MaterialTheme.typography.bodyLarge
                )
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

@Composable
@Preview
fun SelectInstallPreview() {
    InstallScreen(EmptyDestinationsNavigator)
}