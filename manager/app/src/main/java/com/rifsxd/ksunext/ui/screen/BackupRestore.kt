package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.util.themeBackup
import com.rifsxd.ksunext.ui.util.themeRestore
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
        // Theme Settings Backup & Restore
        item {
            var isThemeBackupLoading by remember { mutableStateOf(false) }
            var isThemeRestoreLoading by remember { mutableStateOf(false) }
            var showBackupPathDialog by remember { mutableStateOf(false) }
            var showRestorePathDialog by remember { mutableStateOf(false) }
            var backupPath by remember { mutableStateOf("/data/adb/ksu/backup/theme") }
            var restorePath by remember { mutableStateOf("/data/adb/ksu/backup/theme") }
            
            val themeBackupDialog = rememberLoadingDialog(
                isLoading = isThemeBackupLoading,
                title = "Theme Backup",
                message = "Creating theme backup..."
            )
            
            val themeRestoreDialog = rememberLoadingDialog(
                isLoading = isThemeRestoreLoading,
                title = "Theme Restore",
                message = "Restoring theme settings..."
            )
            
            // Backup Path Dialog
            if (showBackupPathDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupPathDialog = false },
                    title = { Text("Select Backup Path") },
                    text = {
                        Column {
                            Text("Enter the path where you want to save the theme backup:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = backupPath,
                                onValueChange = { backupPath = it },
                                label = { Text("Backup Path") },
                                placeholder = { Text("/data/adb/ksu/backup/theme") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showBackupPathDialog = false
                                scope.launch {
                                    isThemeBackupLoading = true
                                    themeBackupDialog.show()
                                    
                                    try {
                                        val success = withContext(Dispatchers.IO) {
                                            themeBackup(backupPath.takeIf { it.isNotBlank() })
                                        }
                                        
                                        if (success) {
                                            snackbarHostState.showSnackbar(
                                                "Theme backup created successfully at $backupPath",
                                                duration = SnackbarDuration.Short
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Failed to create theme backup",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Error creating theme backup: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    } finally {
                                        isThemeBackupLoading = false
                                        themeBackupDialog.hide()
                                    }
                                }
                            }
                        ) {
                            Text("Backup")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBackupPathDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Restore Path Dialog
            if (showRestorePathDialog) {
                AlertDialog(
                    onDismissRequest = { showRestorePathDialog = false },
                    title = { Text("Select Restore Path") },
                    text = {
                        Column {
                            Text("Enter the path where your theme backup is located:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = restorePath,
                                onValueChange = { restorePath = it },
                                label = { Text("Restore Path") },
                                placeholder = { Text("/data/adb/ksu/backup/theme") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestorePathDialog = false
                                scope.launch {
                                    isThemeRestoreLoading = true
                                    themeRestoreDialog.show()
                                    
                                    try {
                                        val success = withContext(Dispatchers.IO) {
                                            themeRestore(restorePath.takeIf { it.isNotBlank() })
                                        }
                                        
                                        if (success) {
                                            snackbarHostState.showSnackbar(
                                                "Theme restored successfully. Restart app to see changes.",
                                                duration = SnackbarDuration.Long
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Failed to restore theme. No backup found or restore failed.",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Error restoring theme: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    } finally {
                                        isThemeRestoreLoading = false
                                        themeRestoreDialog.hide()
                                    }
                                }
                            }
                        ) {
                            Text("Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestorePathDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            StandardCard {
                Text(
                    text = "Theme Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                CardItemSpacer()
                
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = "Backup Theme",
                    subtitle = "Save theme settings and background image to a tar file",
                    modifier = Modifier.clickable(
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    ) {
                        showBackupPathDialog = true
                    }
                )
                
                CardItemSpacer()
                
                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = "Restore Theme",
                    subtitle = "Restore theme settings from the latest backup tar file",
                    modifier = Modifier.clickable(
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    ) {
                        showRestorePathDialog = true
                    }
                )
            }
        }
        
        item {
            StandardCard {

                val moduleBackup = stringResource(id = R.string.module_backup)
                val backupMessage = stringResource(id = R.string.module_backup_message)
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = moduleBackup,
                    modifier = Modifier.clickable(
                        onClick = {
                            scope.launch {
                                val result = backupDialog.awaitConfirm(title = moduleBackup, content = backupMessage)
                                if (result == ConfirmResult.Confirmed) {
                                    loadingDialog.withLoading {
                                        moduleBackup()
                                    }
                                }
                            }
                        },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                val moduleRestore = stringResource(id = R.string.module_restore)
                val restoreMessage = stringResource(id = R.string.module_restore_message)

                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = moduleRestore,
                    modifier = Modifier.clickable(
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
                    modifier = Modifier.clickable(
                        onClick = {
                            scope.launch {
                                val result = backupDialog.awaitConfirm(title = allowlistBackup, content = allowlistBackupMessage)
                                if (result == ConfirmResult.Confirmed) {
                                    loadingDialog.withLoading {
                                        allowlistBackup()
                                    }
                                }
                            }
                        },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                val allowlistRestore = stringResource(id = R.string.allowlist_restore)
                val allowlistRestoreMessage = stringResource(id = R.string.allowlist_restore_message)
                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = allowlistRestore,
                    modifier = Modifier.clickable(
                        onClick = {
                            scope.launch {
                                val result = restoreDialog.awaitConfirm(title = allowlistRestore, content = allowlistRestoreMessage)
                                if (result == ConfirmResult.Confirmed) {
                                    loadingDialog.withLoading {
                                        allowlistRestore()
                                    }
                                }
                            }
                        },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
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
