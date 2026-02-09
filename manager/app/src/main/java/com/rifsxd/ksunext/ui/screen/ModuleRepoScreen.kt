package com.rifsxd.ksunext.ui.screen

import android.net.Uri
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.R
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RepoManagerScreenDestination
import com.rifsxd.ksunext.ui.viewmodel.ModuleRepoViewModel
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.SearchAppBar
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.rifsxd.ksunext.ui.util.LocalUiOverlaySettings
import com.rifsxd.ksunext.ui.util.LocalBaseColorScheme
import com.rifsxd.ksunext.ui.util.download
import com.rifsxd.ksunext.ui.util.DownloadListener
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.Download

import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview

import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ModuleRepoScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<ModuleRepoViewModel>()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = remember { LazyListState() }
    var showFab by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currIndex, currOffset) ->
                val isScrollingDown = currIndex > lastIndex ||
                        (currIndex == lastIndex && currOffset > lastOffset + 4)
                val isScrollingUp = currIndex < lastIndex ||
                        (currIndex == lastIndex && currOffset < lastOffset - 4)

                when {
                    isScrollingDown && showFab -> showFab = false
                    isScrollingUp && !showFab -> showFab = true
                }

                lastIndex = currIndex
                lastOffset = currOffset
            }
    }

    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }

    val onInstallModule: (Uri) -> Unit = { uri ->
        navigator.navigate(
            FlashScreenDestination(FlashIt.FlashModules(listOf(uri)))
        )
    }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.reloadRepositories()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.repoState is ModuleRepoViewModel.RepoState.Loading) {
            viewModel.fetchModules()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            SearchAppBar(
                title = { Text("Module Repo") },
                searchText = viewModel.searchQuery,
                onSearchTextChange = { viewModel.updateSearchQuery(it) },
                onClearClick = { viewModel.updateSearchQuery("") },
                onBackClick = { navigator.popBackStack() },
                dropdownContent = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.SortByAlpha,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    viewModel.updateSortOption(ModuleRepoViewModel.SortOption.NAME_ASC)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (viewModel.sortOption == ModuleRepoViewModel.SortOption.NAME_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    viewModel.updateSortOption(ModuleRepoViewModel.SortOption.NAME_DESC)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (viewModel.sortOption == ModuleRepoViewModel.SortOption.NAME_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Newest)") },
                                onClick = {
                                    viewModel.updateSortOption(ModuleRepoViewModel.SortOption.DATE_NEWEST)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (viewModel.sortOption == ModuleRepoViewModel.SortOption.DATE_NEWEST) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest)") },
                                onClick = {
                                    viewModel.updateSortOption(ModuleRepoViewModel.SortOption.DATE_OLDEST)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (viewModel.sortOption == ModuleRepoViewModel.SortOption.DATE_OLDEST) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Installed") },
                                onClick = {
                                    viewModel.updateSortOption(ModuleRepoViewModel.SortOption.INSTALLED)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (viewModel.sortOption == ModuleRepoViewModel.SortOption.INSTALLED) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(
                    animationSpec = tween(200),
                    initialScale = 0.8f
                ) + fadeIn(animationSpec = tween(400)),
                exit = scaleOut(
                    animationSpec = tween(200),
                    targetScale = 0.8f
                ) + fadeOut(animationSpec = tween(400))
            ) {
                FloatingActionButton(
                    onClick = { navigator.navigate(RepoManagerScreenDestination) },
                ) {
                    Icon(Icons.Default.Storage, contentDescription = "Manage Repos")
                }
            }
        }
    ) { innerPadding ->
        val state = viewModel.repoState
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            onRefresh = {
                viewModel.fetchModules()
            },
            isRefreshing = viewModel.isRefreshing
        ) {
            when (state) {
                is ModuleRepoViewModel.RepoState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!viewModel.isRefreshing) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ModuleRepoViewModel.RepoState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}")
                            Button(onClick = { viewModel.fetchModules() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is ModuleRepoViewModel.RepoState.Success -> {
                    if (state.modules.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (viewModel.searchQuery.isNotEmpty()) "No modules match your search." else "No modules found.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            if (viewModel.searchQuery.isEmpty()) {
                                Text(
                                    "Check your repositories or internet connection.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { navigator.navigate(RepoManagerScreenDestination) }) {
                                    Text("Manage Repositories")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { viewModel.fetchModules() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, 
                                end = 16.dp, 
                                top = 16.dp,
                                bottom = 16.dp // Reduced from potentially double padding
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        items(state.modules) { module ->
                            var isDownloading by remember { mutableStateOf(false) }
                            var showVersionDialog by remember { mutableStateOf(false) }
                            val baseScheme = LocalBaseColorScheme.current
                            val cardAlpha = LocalUiOverlaySettings.current.cardAlpha

                            if (showVersionDialog) {
                                Dialog(
                                    onDismissRequest = { showVersionDialog = false }
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val view = LocalView.current
                                        DisposableEffect(view) {
                                            val root = view.rootView
                                            val params = root.layoutParams as? WindowManager.LayoutParams
                                            if (params != null) {
                                                params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                                                params.blurBehindRadius = 60
                                                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                                wm.updateViewLayout(root, params)
                                            }
                                            onDispose {}
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(0.95f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = baseScheme.surfaceContainer.copy(alpha = cardAlpha),
                                        ),
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                            Text(
                                                text = module.name,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            if (module.versions.isNotEmpty()) {
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.heightIn(max = 400.dp)
                                                ) {
                                                    items(module.versions) { ver ->
                                                        Card(
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                            ),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .padding(16.dp)
                                                                    .fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                                    Text(
                                                                        text = "${ver.version} (${ver.versionCode})",
                                                                        style = MaterialTheme.typography.titleMedium
                                                                    )
                                                                    if (ver.changelog.isNotEmpty()) {
                                                                        Spacer(Modifier.height(4.dp))
                                                                        Text(
                                                                            text = ver.changelog,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            maxLines = 3,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }
                                                                
                                                                Button(
                                                                    onClick = {
                                                                        if (ver.zipUrl.isNotEmpty()) {
                                                                            scope.launch(Dispatchers.IO) {
                                                                                val uri = viewModel.downloadModule(ver.zipUrl, module.id, ver.version, context)
                                                                                withContext(Dispatchers.Main) {
                                                                                    if (uri != null) {
                                                                                        showVersionDialog = false
                                                                                        navigator.navigate(
                                                                                            FlashScreenDestination(FlashIt.FlashModules(listOf(uri)))
                                                                                        )
                                                                                    } else {
                                                                                        showVersionDialog = false
                                                                                        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                                    ),
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Download,
                                                                        contentDescription = "Install"
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text("No version history available.")
                                            }
                                            
                                            Spacer(modifier = Modifier.height(24.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = { showVersionDialog = false }) {
                                                    Text("Close")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showVersionDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = baseScheme.surfaceContainer.copy(alpha = cardAlpha),
                                )
                            ) {
                                Column(
                                        modifier = Modifier
                                            .padding(22.dp, 18.dp, 22.dp, 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(0.8f)
                                            ) {
                                                if (viewModel.installedModules.containsKey(module.id)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        LabelItem(
                                                            text = "Installed: ${viewModel.installedModules[module.id]}",
                                                            style = LabelItemDefaults.style.copy(
                                                                containerColor = MaterialTheme.colorScheme.primary,
                                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                                            )
                                                        )
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                } else {
                                                    // Add spacer if no labels to maintain alignment if desired, 
                                                    // but Module.kt only adds spacer if there are labels? 
                                                    // Actually Module.kt adds spacer after the Label Row regardless.
                                                    // But here Label Row is conditional. 
                                                    // Let's stick to adding spacer only if installed for now, or maybe not?
                                                    // Module.kt: Row(Labels) -> Spacer(8.dp) -> Text(Name).
                                                    // If no labels, the Row is empty? No, Module.kt checks conditions.
                                                    // If we want exact spacing, we should probably not add spacer if nothing is above.
                                                }

                                                Text(
                                                    text = module.name,
                                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily
                                                )

                                                Text(
                                                    text = "${stringResource(R.string.module_version)}: ${module.version}",
                                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                                )
                                                
                                                Text(
                                                    text = "${stringResource(R.string.module_author)}: ${module.author}",
                                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                                )

                                                if (module.timestamp > 0) {
                                                    Text(
                                                        text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(module.timestamp)),
                                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                if (isDownloading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            if (module.zipUrl.isNotEmpty()) {
                                                                scope.launch(Dispatchers.IO) {
                                                                    val uri = viewModel.downloadModule(module.zipUrl, module.id, module.version, context)
                                                                    withContext(Dispatchers.Main) {
                                                                        if (uri != null) {
                                                                            navigator.navigate(
                                                                                FlashScreenDestination(FlashIt.FlashModules(listOf(uri)))
                                                                            )
                                                                        } else {
                                                                            Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        ),
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Download,
                                                            contentDescription = "Install"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    
                                        if (!module.description.equals("none", ignoreCase = true)) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = module.description,
                                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 4
                                            )
                                        }
                                    }
                            }
                        }
                    }
                    }
                }
            }
        }
        DownloadListener(context, onInstallModule)
    }
}
