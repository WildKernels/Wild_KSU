package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.component.ConfirmResult
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import com.rifsxd.ksunext.ui.component.CardConstants
import com.rifsxd.ksunext.ui.component.rememberNoRippleInteractionSource
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberLoadingDialog
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.launch

/**
 * @author rifsxd
 * @date 2025/1/14.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.becomeManager(ksuApp.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { paddingValues ->
        val loadingDialog = rememberLoadingDialog()
        val restoreDialog = rememberConfirmDialog()
        val backupDialog = rememberConfirmDialog()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var showRebootDialog by remember { mutableStateOf(false) }

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

            // Keep UI minimal: two cards only

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

            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            var useOverlayFs by rememberSaveable {
                mutableStateOf(readMountSystemFile())
            }

            // Module card: Restore + Backup in one card
            StandardCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_MEDIUM)
                ) {
                    val moduleRestore = stringResource(id = R.string.module_restore)
                    val restoreMessage = stringResource(id = R.string.module_restore_message)
                    ListItem(
                        modifier = if (!useOverlayFs) {
                            Modifier.clickable(
                                onClick = {
                                    scope.launch {
                                        val result = restoreDialog.awaitConfirm(title = moduleRestore, content = restoreMessage)
                                        if (result == ConfirmResult.Confirmed) {
                                            loadingDialog.withLoading {
                                                moduleRestore()
                                                showRebootDialog = true
                                            }
                                        }
                                    }
                                },
                                interactionSource = rememberNoRippleInteractionSource(),
                                indication = null
                            )
                        } else Modifier,
                        leadingContent = {
                            Icon(
                                Icons.Filled.Restore,
                                moduleRestore,
                                tint = if (useOverlayFs) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        headlineContent = {
                            Text(
                                moduleRestore,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (useOverlayFs) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )

                    CardItemSpacer()

                    val moduleBackupText = stringResource(id = R.string.module_backup)
                    val moduleBackupMessage = stringResource(id = R.string.module_backup_message)
                    ListItem(
                        modifier = Modifier.clickable(
                            onClick = {
                                scope.launch {
                                    val result = backupDialog.awaitConfirm(title = moduleBackupText, content = moduleBackupMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            moduleBackup()
                                        }
                                    }
                                }
                            },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        ),
                        leadingContent = {
                            Icon(
                                Icons.Filled.Backup,
                                moduleBackupText
                            )
                        },
                        headlineContent = {
                            Text(
                                text = moduleBackupText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    )
                }
            }

            CardItemSpacer()

            // Allowlist card: Backup + Restore in one card
            StandardCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_MEDIUM)
                ) {
                    val allowlistBackupText = stringResource(id = R.string.allowlist_backup)
                    val allowlistBackupMessage = stringResource(id = R.string.allowlist_backup_message)
                    ListItem(
                        modifier = Modifier.clickable(
                            onClick = {
                                scope.launch {
                                    val result = backupDialog.awaitConfirm(title = allowlistBackupText, content = allowlistBackupMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            allowlistBackup()
                                        }
                                    }
                                }
                            },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        ),
                        leadingContent = {
                            Icon(
                                Icons.Filled.Backup,
                                allowlistBackupText
                            )
                        },
                        headlineContent = {
                            Text(
                                text = allowlistBackupText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    )

                    CardItemSpacer()

                    val allowlistRestore = stringResource(id = R.string.allowlist_restore)
                    val allowlistrestoreMessage = stringResource(id = R.string.allowlist_restore_message)
                    ListItem(
                        modifier = Modifier.clickable(
                            onClick = {
                                scope.launch {
                                    val result = restoreDialog.awaitConfirm(title = allowlistRestore, content = allowlistrestoreMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            allowlistRestore()
                                        }
                                    }
                                }
                            },
                            interactionSource = rememberNoRippleInteractionSource(),
                            indication = null
                        ),
                        leadingContent = {
                            Icon(
                                Icons.Filled.Restore,
                                allowlistRestore
                            )
                        },
                        headlineContent = {
                            Text(
                                text = allowlistRestore,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    )
                }
            }
        }
    }
}



@Preview
@Composable
private fun BackupPreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
