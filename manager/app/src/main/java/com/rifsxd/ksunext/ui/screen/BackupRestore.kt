package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import com.rifsxd.ksunext.ui.component.CardRowContent
import com.rifsxd.ksunext.ui.component.ConfirmResult
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberLoadingDialog
import com.rifsxd.ksunext.ui.component.rememberNoRippleInteractionSource
import com.rifsxd.ksunext.ui.util.allowlistBackup
import com.rifsxd.ksunext.ui.util.allowlistRestore
import com.rifsxd.ksunext.ui.util.moduleBackup
import com.rifsxd.ksunext.ui.util.moduleRestore
import com.rifsxd.ksunext.ui.util.readMountSystemFile
import com.rifsxd.ksunext.ui.util.reboot
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val loadingDialog = rememberLoadingDialog()
    val restoreDialog = rememberConfirmDialog()
    val backupDialog = rememberConfirmDialog()
    
    var showRebootDialog by remember { mutableStateOf(false) }
    var useOverlayFs by rememberSaveable { mutableStateOf(readMountSystemFile()) }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.reboot_required),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StandardCard {

                val moduleBackup = stringResource(id = R.string.module_backup)
                val backupMessage = stringResource(id = R.string.module_backup_message)
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = moduleBackup,
                    onClick = {
                        scope.launch {
                            val result = backupDialog.awaitConfirm(title = moduleBackup, content = backupMessage)
                            if (result == ConfirmResult.Confirmed) {
                                loadingDialog.withLoading {
                                    moduleBackup()
                                }
                            }
                        }
                    }
                )

                CardItemSpacer()

                val moduleRestore = stringResource(id = R.string.module_restore)
                val restoreMessage = stringResource(id = R.string.module_restore_message)

                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = moduleRestore,
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
                    }
                )

            }
        }
        
        item {
            StandardCard {
                val allowlistBackup = stringResource(id = R.string.allowlist_backup)
                val allowlistBackupMessage = stringResource(id = R.string.allowlist_backup_message)
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = allowlistBackup,
                    onClick = {
                        scope.launch {
                            val result = backupDialog.awaitConfirm(title = allowlistBackup, content = allowlistBackupMessage)
                            if (result == ConfirmResult.Confirmed) {
                                loadingDialog.withLoading {
                                    allowlistBackup()
                                }
                            }
                        }
                    }
                )

                CardItemSpacer()

                val allowlistRestore = stringResource(id = R.string.allowlist_restore)
                val allowlistRestoreMessage = stringResource(id = R.string.allowlist_restore_message)
                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = allowlistRestore,
                    onClick = {
                        scope.launch {
                            val result = restoreDialog.awaitConfirm(title = allowlistRestore, content = allowlistRestoreMessage)
                            if (result == ConfirmResult.Confirmed) {
                                loadingDialog.withLoading {
                                    allowlistRestore()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun BackupPreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
