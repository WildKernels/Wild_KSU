package com.rifsxd.ksunext.ui

import android.content.Intent
import android.net.Uri
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.SharedPreferences
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.destinations.ExecuteModuleActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomizationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupRestoreScreenDestination
import com.ramcosta.composedestinations.generated.NavGraphs
import androidx.navigation.NavDestination
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.screen.BottomBarDestination
import com.rifsxd.ksunext.ui.theme.KernelSUTheme
import com.rifsxd.ksunext.ui.component.BackgroundImageWrapper
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.LocaleHelper
import com.rifsxd.ksunext.ui.util.rootAvailable
import com.rifsxd.ksunext.ui.util.install
import com.rifsxd.ksunext.ui.util.isSuCompatDisabled
import com.rifsxd.ksunext.ui.screen.FlashIt
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Enable edge to edge
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val isManager = Natives.becomeManager(ksuApp.packageName)
        if (isManager) install()

        val zipUri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW, Intent.ACTION_SEND -> {
                val uri = intent.data ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                uri?.let {
                    val name = when (it.scheme) {
                        "file" -> it.lastPathSegment ?: ""
                        "content" -> {
                            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (cursor.moveToFirst() && nameIndex != -1) {
                                    cursor.getString(nameIndex)
                                } else {
                                    it.lastPathSegment ?: ""
                                }
                            } ?: (it.lastPathSegment ?: "")
                        }
                        else -> it.lastPathSegment ?: ""
                    }
                    if (name.lowercase().endsWith(".zip")) it else null
                }
            }
            else -> null
        }

        setContent {
            // Get SharedPreferences
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            
            // Use remember and mutableStateOf for reactive preferences
            // Darkness slider: 1.0f = 100% (full black overlay), 0.0f = 0% (no overlay)
            // UI transparency: 0.0f = 0% (fully transparent), 1.0f = 100% (fully opaque)
            var amoledMode by remember { mutableStateOf(prefs.getBoolean("enable_amoled", false)) }
            var backgroundImageUri by remember { mutableStateOf(prefs.getString("background_image_uri", null)) }
            var backgroundTransparency by remember { mutableStateOf(prefs.getFloat("background_transparency", 1.0f)) } // Default 100% darkness
            var uiTransparency by remember { mutableStateOf(prefs.getFloat("ui_transparency", 0.0f)) } // Default 0% UI transparency
            
            // Listen for preference changes
            LaunchedEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "enable_amoled" -> {
                            amoledMode = prefs.getBoolean("enable_amoled", false)
                        }
                        "background_image_uri" -> {
                            backgroundImageUri = prefs.getString("background_image_uri", null)
                        }
                        "background_transparency" -> {
                            backgroundTransparency = prefs.getFloat("background_transparency", 1.0f)
                        }
                        "ui_transparency" -> {
                            uiTransparency = prefs.getFloat("ui_transparency", 0.0f)
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
            }

            val moduleViewModel: ModuleViewModel = viewModel()
            val superUserViewModel: SuperUserViewModel = viewModel()
            val moduleUpdateCount = moduleViewModel.moduleList.count { 
                moduleViewModel.checkUpdate(it).first.isNotEmpty()
            }

            KernelSUTheme (
                amoledMode = amoledMode,
                isCustomBackgroundEnabled = !backgroundImageUri.isNullOrEmpty(),
                uiTransparency = uiTransparency
            ) {
                BackgroundImageWrapper(
                    backgroundImageUri = backgroundImageUri,
                    backgroundFitMode = "custom_crop", // Default to advanced crop editor
                    backgroundTransparency = backgroundTransparency
                ) {
                val navController = rememberNavController()
                val snackBarHostState = remember { SnackbarHostState() }
                val currentDestination = navController.currentBackStackEntryAsState()?.value?.destination

                val navigator = navController.rememberDestinationsNavigator()

                LaunchedEffect(zipUri) {
                    if (zipUri != null) {
                        navigator.navigate(
                            FlashScreenDestination(
                                FlashIt.FlashModules(listOf(zipUri)),
                                finishIntent = true
                            )
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    if (superUserViewModel.appList.isEmpty()) {
                        superUserViewModel.fetchAppList()
                    }

                    if (moduleViewModel.moduleList.isEmpty()) {
                        moduleViewModel.fetchModuleList()
                    }
                }

                val showBottomBar = when (currentDestination?.route) {
                    FlashScreenDestination.route -> false // Hide for FlashScreenDestination
                    ExecuteModuleActionScreenDestination.route -> false // Hide for ExecuteModuleActionScreen
                    else -> true
                }

                val showTopBar = when (currentDestination?.route) {
                    FlashScreenDestination.route -> false // Hide for FlashScreenDestination
                    ExecuteModuleActionScreenDestination.route -> false // Hide for ExecuteModuleActionScreen
                    ModuleScreenDestination.route -> false // Hide for ModuleScreen (has its own SearchAppBar)
                    SuperUserScreenDestination.route -> false // Hide for SuperUserScreen (has its own SearchAppBar)
                    else -> true
                }

                Scaffold(
                    topBar = {
                        AnimatedVisibility(
                            visible = showTopBar,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            TopBar(navController, currentDestination)
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            BottomBar(navController, moduleUpdateCount)
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                    ) {
                        DestinationsNavHost(
                            modifier = Modifier.padding(innerPadding),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                                override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
                                    get() = { fadeIn(animationSpec = tween(340)) }
                                override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
                                    get() = { fadeOut(animationSpec = tween(340)) }
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, moduleUpdateCount: Int) {
    val navigator = navController.rememberDestinationsNavigator()
    val isManager = Natives.becomeManager(ksuApp.packageName)
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()
    val suCompatDisabled = isSuCompatDisabled()
    val suSFS = getSuSFS()
    val susSUMode = susfsSUS_SU_Mode()

    NavigationBar(
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) {
        BottomBarDestination.entries
            .forEach { destination ->
                if (!fullFeatured && destination.rootRequired) return@forEach
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(destination.direction, false)
                        }
                        navigator.navigate(destination.direction) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        // Show badge for Module icon if moduleUpdateCount > 0
                        if (destination == BottomBarDestination.Module && moduleUpdateCount > 0) {
                            BadgedBox(badge = { Badge { Text(moduleUpdateCount.toString()) } }) {
                                if (isCurrentDestOnBackStack) {
                                    Icon(destination.iconSelected, stringResource(destination.label))
                                } else {
                                    Icon(destination.iconNotSelected, stringResource(destination.label))
                                }
                            }
                        } else {
                            if (isCurrentDestOnBackStack) {
                                Icon(destination.iconSelected, stringResource(destination.label))
                            } else {
                                Icon(destination.iconNotSelected, stringResource(destination.label))
                            }
                        }
                    },
                    label = { Text(stringResource(destination.label)) },
                    alwaysShowLabel = true
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, currentDestination: NavDestination?) {
    val navigator = navController.rememberDestinationsNavigator()
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    // Determine if we need a back button and the title based on current destination
    val (title, showBackButton) = when (currentDestination?.route) {
        HomeScreenDestination.route -> stringResource(R.string.app_name) to false
        ModuleScreenDestination.route -> stringResource(R.string.module) to false
        SuperUserScreenDestination.route -> stringResource(R.string.superuser) to false
        SettingScreenDestination.route -> stringResource(R.string.settings) to false
        CustomizationScreenDestination.route -> stringResource(R.string.customization) to true
        DeveloperScreenDestination.route -> stringResource(R.string.developer) to true
        BackupRestoreScreenDestination.route -> stringResource(R.string.backup_restore) to true
        AppProfileScreenDestination.route -> stringResource(R.string.profile) to true
        TemplateEditorScreenDestination.route -> stringResource(R.string.app_profile_template_edit) to true
        AppProfileTemplateScreenDestination.route -> stringResource(R.string.settings_profile_template) to true
        ExecuteModuleActionScreenDestination.route -> stringResource(R.string.action) to true
        InstallScreenDestination.route -> stringResource(R.string.install) to true
        FlashScreenDestination.route -> stringResource(R.string.install) to true
        else -> "" to false
    }
    
    TopAppBar(
        title = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) 
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = { navigator.navigateUp() }
                ) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) 
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}
