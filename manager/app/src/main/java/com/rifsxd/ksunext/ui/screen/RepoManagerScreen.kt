package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.viewmodel.ModuleRepoViewModel
import com.rifsxd.ksunext.ui.util.LocalBaseColorScheme
import androidx.compose.ui.graphics.Color
import com.rifsxd.ksunext.ui.component.BlurDialog

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RepoManagerScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<ModuleRepoViewModel>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("") }
    var newRepoUrl by remember { mutableStateOf("") }

    val confirmReset = rememberConfirmDialog(onConfirm = {
        viewModel.resetRepositories()
    })
    
    var repoToDelete by remember { mutableStateOf<ModuleRepoViewModel.Repository?>(null) }
    val confirmDelete = rememberConfirmDialog(onConfirm = {
        repoToDelete?.let { viewModel.removeRepository(it) }
        repoToDelete = null
    })

    if (showAddRepoDialog) {
        BlurDialog(onDismissRequest = { showAddRepoDialog = false }) {
            Text(
                text = "Add Repository",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newRepoName,
                onValueChange = { newRepoName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newRepoUrl,
                onValueChange = { newRepoUrl = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newRepoName.isNotBlank() && newRepoUrl.isNotBlank()) {
                            viewModel.addRepository(newRepoName, newRepoUrl)
                            newRepoName = ""
                            newRepoUrl = ""
                            showAddRepoDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text("Manage Repositories") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        confirmReset.showConfirm(
                            title = "Reset Repositories",
                            content = "Are you sure you want to reset to default repositories? This cannot be undone.",
                            confirm = "Reset",
                            dismiss = "Cancel"
                        )
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to Default")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddRepoDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Repository")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(viewModel.repositories) { repo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                repo.name, 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                repo.url, 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = repo.enabled,
                            onCheckedChange = { viewModel.toggleRepository(repo) }
                        )
                        IconButton(onClick = { 
                            repoToDelete = repo
                            confirmDelete.showConfirm(
                                title = "Delete Repository",
                                content = "Are you sure you want to delete '${repo.name}'?",
                                confirm = "Delete",
                                dismiss = "Cancel"
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        confirmReset.showConfirm(
                            title = "Reset Repositories",
                            content = "Are you sure you want to reset to default repositories? This cannot be undone.",
                            confirm = "Reset",
                            dismiss = "Cancel"
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
