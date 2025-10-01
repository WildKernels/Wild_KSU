package com.rifsxd.ksunext.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.SharedPreferences
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.compositionLocalOf
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ExecuteModuleActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomizationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupRestoreScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TemplateEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppProfileTemplateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperuserSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PhotoEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeSettingsScreenDestination
import androidx.navigation.NavDestination
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.getKernelVersion
import com.rifsxd.ksunext.ui.screen.BottomBarDestination
import com.rifsxd.ksunext.ui.screen.FlashIt
import com.rifsxd.ksunext.ui.theme.KernelSUTheme
import com.rifsxd.ksunext.ui.component.BackgroundImageWrapper
import com.rifsxd.ksunext.ui.component.SearchAppBar
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel
import com.rifsxd.ksunext.ui.viewmodel.FlashViewModel
import com.rifsxd.ksunext.ui.viewmodel.FlashingStatus
import kotlinx.coroutines.launch
import java.util.*

// CompositionLocal providers for ViewModels
val LocalModuleViewModel = compositionLocalOf<ModuleViewModel> { error("ModuleViewModel not provided") }
val LocalSuperUserViewModel = compositionLocalOf<SuperUserViewModel> { error("SuperUserViewModel not provided") }
val LocalFlashViewModel = compositionLocalOf<FlashViewModel> { error("FlashViewModel not provided") }


// Icon type enum
enum class IconType(val displayName: String, val icon: ImageVector) {
    OFF("Off", Icons.Filled.VisibilityOff), // Icons disabled
    SEASONAL("Seasonal", Icons.Filled.Whatshot), // Placeholder, actual icon determined by season
    WINTER("Winter", Icons.Filled.AcUnit),
    SPRING("Spring", Icons.Filled.Spa),
    SUMMER("Summer", Icons.Filled.WbSunny),
    FALL("Fall", Icons.Filled.Forest),
    KSU_NEXT("KSU Next", Icons.Filled.Whatshot), // Placeholder, actual icon is drawable
    CANNABIS("Cannabis", Icons.Filled.Whatshot), // Placeholder, actual icon is drawable
    AMOGUS_SUSFS("Amogus", Icons.Filled.Whatshot) // Placeholder, actual icon is drawable
}

// Get icon based on type and season
@Composable
private fun getIcon(iconType: IconType): Any {
    return when (iconType) {
        IconType.OFF -> Icons.Filled.VisibilityOff
        IconType.SEASONAL -> getSeasonalIcon()
        IconType.WINTER -> Icons.Filled.AcUnit
        IconType.SPRING -> Icons.Filled.Spa
        IconType.SUMMER -> Icons.Filled.WbSunny
        IconType.FALL -> Icons.Filled.Forest
        IconType.KSU_NEXT -> painterResource(R.drawable.ic_ksu_next)
        IconType.CANNABIS -> painterResource(R.drawable.ic_cannabis)
        IconType.AMOGUS_SUSFS -> painterResource(R.drawable.ic_sus)
    }
}

// Seasonal icon function
private fun getSeasonalIcon(): ImageVector {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH)
    
    return when (month) {
        Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Icons.Filled.AcUnit // Winter
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Icons.Filled.Spa // Spring
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Icons.Filled.WbSunny // Summer
        Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Icons.Filled.Forest // Fall
        else -> Icons.Filled.Whatshot // Fallback icon
    }
}



class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let { LocaleHelper.applyLanguage(it) }
        val scaledContext = context?.let { applyDpiScale(it) }
        super.attachBaseContext(scaledContext)
    }
    
    private fun applyDpiScale(context: Context): Context {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val dpiValue = prefs.getInt("app_dpi", 0)
        
        if (dpiValue == 0) {
            return context
        }
        
        val configuration = context.resources.configuration
        configuration.densityDpi = dpiValue
        
        return context.createConfigurationContext(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Enable edge to edge
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val isManager = Natives.becomeManager(packageName)
        if (isManager) {
            install()
            
            // Synchronize overlay FS preference with actual system state
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val actualOverlayFsState = readMountSystemFile()
            val preferenceOverlayFsState = prefs.getBoolean("use_overlay_fs", false)
            
            // If there's a mismatch, update the preference to match the actual state
            if (actualOverlayFsState != preferenceOverlayFsState) {
                prefs.edit().putBoolean("use_overlay_fs", actualOverlayFsState).apply()
            }
        }

        val zipUri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW, Intent.ACTION_SEND -> {
                val uri = intent.data ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
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
            var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system_default") ?: "system_default") }
            var backgroundImageUri by remember { 
                mutableStateOf(
                    // Check if we have an internal storage path first
                    BackgroundCustomization.getInternalBackgroundImagePath(this@MainActivity)?.let {
                        BackgroundCustomization.filePathToUri(it) 
                    } ?: prefs.getString("background_image_uri", null)
                )
            }
            var backgroundTransparency by remember { mutableStateOf(prefs.getFloat("background_transparency", 0.0f)) } // Default 0% darkness (fully visible)
            var uiTransparency by remember { mutableStateOf(prefs.getFloat("ui_transparency", 0.0f)) } // Default 0% UI transparency

            var backgroundBlur by remember { mutableStateOf(prefs.getFloat("background_blur", 0.0f)) } // Default 0px blur
            var backgroundFitMode by remember { mutableStateOf(prefs.getString("background_fit_mode", "fit") ?: "fit") }
            
            // DPI setting
            var appDpi by remember { mutableStateOf(prefs.getInt("app_dpi", 0)) }
            
            // Icon settings
            var selectedIconType by remember { 
                mutableStateOf(
                    IconType.values().find { it.name == prefs.getString("selected_icon_type", "SEASONAL") } ?: IconType.SEASONAL
                )
            }
            
            // Listen for preference changes
            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme_mode" -> {
                            themeMode = prefs.getString("theme_mode", "system_default") ?: "system_default"
                        }
                        "enable_amoled" -> {
                            // For backward compatibility, update theme_mode when enable_amoled changes
                            val isAmoled = prefs.getBoolean("enable_amoled", false)
                            if (isAmoled && themeMode != "amoled") {
                                themeMode = "amoled"
                            } else if (!isAmoled && themeMode == "amoled") {
                                themeMode = "system_default"
                            }
                        }
                        "background_image_uri" -> {
                            // Check internal storage first, then fallback to preferences
                            val internalPath = BackgroundCustomization.getInternalBackgroundImagePath(this@MainActivity)
                            val prefsUri = prefs.getString("background_image_uri", null)
                            
                            
                            backgroundImageUri = internalPath?.let {
                                BackgroundCustomization.filePathToUri(it) 
                            } ?: prefsUri
                        }
                        "background_transparency" -> {
                            backgroundTransparency = prefs.getFloat("background_transparency", 0.0f)
                        }
                        "ui_transparency" -> {
                            uiTransparency = prefs.getFloat("ui_transparency", 0.0f)
                        }

                        "background_blur" -> {
                            backgroundBlur = prefs.getFloat("background_blur", 0.0f)
                        }
                        "background_fit_mode" -> {
                            backgroundFitMode = prefs.getString("background_fit_mode", "fit") ?: "fit"
                        }
                        "app_dpi" -> {
                            appDpi = prefs.getInt("app_dpi", 0)
                            // Restart activity to apply DPI changes
                            recreate()
                        }
                        "selected_icon_type" -> {
                            selectedIconType = IconType.values().find { it.name == prefs.getString("selected_icon_type", "SEASONAL") } ?: IconType.SEASONAL
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val moduleViewModel: ModuleViewModel = viewModel()
            val superUserViewModel: SuperUserViewModel = viewModel()
            val flashViewModel: FlashViewModel = viewModel()
            val moduleUpdateCount = moduleViewModel.moduleList.count { 
                moduleViewModel.checkUpdate(it).first.isNotEmpty()
            }

            // Calculate theme parameters based on theme mode
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark", "amoled" -> true
                else -> isSystemInDarkTheme() // system_default
            }
            
            val isAmoledMode = themeMode == "amoled"
            // Dynamic colors should be default when available (Android 12+), except when explicitly disabled
            val isDynamicColor = when (themeMode) {
                "light", "dark" -> false // Explicitly disabled dynamic colors
                "amoled" -> true // AMOLED mode with dynamic colors when available
                else -> true // system_default: prefer dynamic colors when available, fallback to system theme
            }
            
            KernelSUTheme (
                darkTheme = isDarkTheme,
                dynamicColor = isDynamicColor,
                amoledMode = isAmoledMode,
                isCustomBackgroundEnabled = !backgroundImageUri.isNullOrEmpty(),
                uiTransparency = uiTransparency
            ) {
                val navController = rememberNavController()
                val snackBarHostState = remember { SnackbarHostState() }
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination

                val navigator = navController.rememberDestinationsNavigator()
                
                // Handle system back navigation
                BackHandler {
                    val currentRoute = currentDestination?.route
                    when (currentRoute) {
                        // Home screen - exit the app
                        HomeScreenDestination.route -> {
                            finish()
                        }
                        // Main screens - navigate to home
                        SuperUserScreenDestination.route,
                        ModuleScreenDestination.route,
                        SettingScreenDestination.route -> {
                            navigator.navigate(HomeScreenDestination) {
                                popUpTo(NavGraphs.root) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        // Sub-screens - navigate to parent
                        CustomizationScreenDestination.route,
                        DeveloperScreenDestination.route,
                        BackupRestoreScreenDestination.route,
                        HomeSettingsScreenDestination.route,
                        ModuleSettingsScreenDestination.route,
                        SuperuserSettingsScreenDestination.route,
                        ThemeSettingsScreenDestination.route -> {
                            navigator.navigate(SettingScreenDestination) {
                                popUpTo(SettingScreenDestination) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                        AppProfileScreenDestination.route,
                        AppProfileTemplateScreenDestination.route -> {
                            navigator.navigate(SuperUserScreenDestination) {
                                popUpTo(SuperUserScreenDestination) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                        InstallScreenDestination.route -> {
                            navigator.navigate(HomeScreenDestination) {
                                popUpTo(HomeScreenDestination) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                        // For other screens, use default back navigation
                        else -> {
                            navigator.navigateUp()
                        }
                    }
                }
                
                // Disable background when in PhotoEditor
                val isInPhotoEditor = currentDestination?.route == PhotoEditorScreenDestination.route
                val effectiveBackgroundUri = if (isInPhotoEditor) null else backgroundImageUri
                
                BackgroundImageWrapper(
                    backgroundImageUri = effectiveBackgroundUri,
                    backgroundFitMode = backgroundFitMode,
                    backgroundTransparency = backgroundTransparency,
                    backgroundBlur = backgroundBlur
                ) {

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

                // Check if bottom bar is disabled in theme settings
                val hideBottomBar = prefs.getBoolean("hide_bottom_bar", false)
                
                val showBottomBar = when {
                    hideBottomBar -> false // Hide if disabled in theme settings
                    currentDestination?.route == FlashScreenDestination.route -> false // Hide for FlashScreenDestination
                    currentDestination?.route == ExecuteModuleActionScreenDestination.route -> false // Hide for ExecuteModuleActionScreen
                    else -> true
                }

                Scaffold(
                    topBar = {
                        // Show unified TopBar for all screens except those that should be hidden
                        val screensWithoutTopBar = listOf(
                            ExecuteModuleActionScreenDestination.route
                        )
                        
                        if (currentDestination?.route !in screensWithoutTopBar) {
                            UnifiedTopBar(
                                currentDestination = currentDestination,
                                navigator = navigator,
                                moduleViewModel = moduleViewModel,
                                superUserViewModel = superUserViewModel,
                                flashViewModel = flashViewModel,
                                selectedIconType = selectedIconType,
                                modifier = Modifier
                            )
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            BottomBar(navController, moduleUpdateCount, Modifier)
                        }
                    },
                    contentWindowInsets = if (showBottomBar) {
                        WindowInsets(0, 0, 0, 0)
                    } else {
                        WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                    }
                ) { innerPadding ->
                    // Create a mutable state for PhotoEditor save callback
                var photoEditorSaveCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var photoEditorResetCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var photoEditorScreenRotationCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var photoEditorScreenRotationLocked by remember { mutableStateOf(false) }
                
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                    LocalModuleViewModel provides moduleViewModel,
                    LocalSuperUserViewModel provides superUserViewModel,
                    LocalFlashViewModel provides flashViewModel,
                    LocalPhotoEditorSaveCallback provides photoEditorSaveCallback,
                    LocalPhotoEditorSaveCallbackSetter provides { callback -> 
                        photoEditorSaveCallback = callback 
                    },
                    LocalPhotoEditorResetCallback provides photoEditorResetCallback,
                    LocalPhotoEditorScreenRotationCallback provides photoEditorScreenRotationCallback,
                    LocalPhotoEditorScreenRotationLocked provides photoEditorScreenRotationLocked
                ) {
                        val currentDestination by navController.currentBackStackEntryAsState()
                        val isPhotoEditor = currentDestination?.destination?.route == PhotoEditorScreenDestination.route
                        
                        DestinationsNavHost(
                            modifier = Modifier
                                .let { if (isPhotoEditor) it else it.padding(innerPadding) },
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
private fun BottomBar(navController: NavHostController, moduleUpdateCount: Int, modifier: Modifier = Modifier) {
    val navigator = navController.rememberDestinationsNavigator()
    val context = LocalContext.current
    val isManager = Natives.becomeManager(context.packageName)
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()
    val suCompatDisabled = isSuCompatDisabled()
    val suSFS = getSuSFS()
    val susSUMode = susfsSUS_SU_Mode()

    NavigationBar(
        modifier = modifier,
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
private fun UnifiedTopBar(
    currentDestination: NavDestination?, 
    navigator: DestinationsNavigator,
    moduleViewModel: ModuleViewModel,
    superUserViewModel: SuperUserViewModel,
    flashViewModel: FlashViewModel,
    selectedIconType: IconType,
    modifier: Modifier = Modifier
) {
    when (currentDestination?.route) {
        ModuleScreenDestination.route -> {
            ModuleTopBar(moduleViewModel = moduleViewModel, modifier = modifier)
        }
        SuperUserScreenDestination.route -> {
            SuperUserTopBar(superUserViewModel = superUserViewModel, navigator = navigator, modifier = modifier)
        }
        HomeSettingsScreenDestination.route -> {
            SettingsTopBar(
                title = stringResource(R.string.info_card_customization),
                navigator = navigator,
                modifier = modifier
            )
        }
        SuperuserSettingsScreenDestination.route -> {
            SettingsTopBar(
                title = "Superuser Settings",
                navigator = navigator,
                modifier = modifier
            )
        }
        ModuleSettingsScreenDestination.route -> {
            SettingsTopBar(
                title = "Module Settings",
                navigator = navigator,
                modifier = modifier
            )
        }
        ThemeSettingsScreenDestination.route -> {
            SettingsTopBar(
                title = "Theme Settings",
                navigator = navigator,
                modifier = modifier
            )
        }
        PhotoEditorScreenDestination.route -> {
            val context = LocalContext.current
            val photoEditorSaveCallback = LocalPhotoEditorSaveCallback.current
            val photoEditorResetCallback = LocalPhotoEditorResetCallback.current
            val photoEditorScreenRotationCallback = LocalPhotoEditorScreenRotationCallback.current
            val photoEditorScreenRotationLocked = LocalPhotoEditorScreenRotationLocked.current
            
            PhotoEditorTopBar(
                navigator = navigator,
                onSave = { photoEditorSaveCallback?.invoke() ?: run {} },
                screenRotationLocked = photoEditorScreenRotationLocked,
                onScreenRotationToggle = { photoEditorScreenRotationCallback?.invoke() ?: run {} },
                modifier = modifier
            )
        }
        TemplateEditorScreenDestination.route -> {
            // TemplateEditor handles its own top bar, so we don't show one here
        }
        BackupRestoreScreenDestination.route -> {
            SettingsTopBar(
                title = "Backup & Restore",
                navigator = navigator,
                modifier = modifier
            )
        }
        else -> {
            RegularTopBar(
                currentDestination = currentDestination, 
                navigator = navigator, 
                flashViewModel = flashViewModel, 
                selectedIconType = selectedIconType,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleTopBar(moduleViewModel: ModuleViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    SearchAppBar(
        modifier = modifier,
        title = { 
            Text(
                text = stringResource(R.string.module),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) 
        },
        searchText = moduleViewModel.search,
        onSearchTextChange = { moduleViewModel.search = it },
                        onClearClick = { moduleViewModel.search = "" },
        dropdownContent = {
            var showDropdown by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showDropdown = true },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.clip(MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    offset = DpOffset(0.dp, 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.module_sort_a_to_z)) },
                        trailingIcon = {
                            Checkbox(checked = moduleViewModel.sortAToZ, onCheckedChange = null)
                        },
                        onClick = {
                            val currentValue = prefs.getBoolean("module_sort_a_to_z", true)
                            prefs.edit()
                                .putBoolean("module_sort_a_to_z", !currentValue)
                                .putBoolean("module_sort_z_to_a", false)
                                .putBoolean("module_sort_size_low_to_high", false)
                                .putBoolean("module_sort_size_high_to_low", false)
                                .putBoolean("module_sort_enabled_first", false)
                                .putBoolean("module_sort_action_first", false)
                                .putBoolean("module_sort_webui_first", false)
                                .commit()
                            moduleViewModel.reloadSortingPreferences(context)
                            scope.launch { moduleViewModel.fetchModuleList() }
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.module_sort_z_to_a)) },
                        trailingIcon = {
                            Checkbox(checked = moduleViewModel.sortZToA, onCheckedChange = null)
                        },
                        onClick = {
                            val currentValue = prefs.getBoolean("module_sort_z_to_a", false)
                            prefs.edit()
                                .putBoolean("module_sort_a_to_z", false)
                                .putBoolean("module_sort_z_to_a", !currentValue)
                                .putBoolean("module_sort_size_low_to_high", false)
                                .putBoolean("module_sort_size_high_to_low", false)
                                .putBoolean("module_sort_enabled_first", false)
                                .putBoolean("module_sort_action_first", false)
                                .putBoolean("module_sort_webui_first", false)
                                .commit()
                            moduleViewModel.reloadSortingPreferences(context)
                            scope.launch { moduleViewModel.fetchModuleList() }
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.module_sort_enabled_first)) },
                        trailingIcon = {
                            Checkbox(checked = moduleViewModel.sortEnabledFirst, onCheckedChange = null)
                        },
                        onClick = {
                            val currentValue = prefs.getBoolean("module_sort_enabled_first", false)
                            prefs.edit()
                                .putBoolean("module_sort_a_to_z", false)
                                .putBoolean("module_sort_z_to_a", false)
                                .putBoolean("module_sort_size_low_to_high", false)
                                .putBoolean("module_sort_size_high_to_low", false)
                                .putBoolean("module_sort_enabled_first", !currentValue)
                                .putBoolean("module_sort_action_first", false)
                                .putBoolean("module_sort_webui_first", false)
                                .commit()
                            moduleViewModel.reloadSortingPreferences(context)
                            scope.launch { moduleViewModel.fetchModuleList() }
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.module_sort_action_first)) },
                        trailingIcon = {
                            Checkbox(checked = moduleViewModel.sortActionFirst, onCheckedChange = null)
                        },
                        onClick = {
                            val currentValue = prefs.getBoolean("module_sort_action_first", false)
                            prefs.edit()
                                .putBoolean("module_sort_a_to_z", false)
                                .putBoolean("module_sort_z_to_a", false)
                                .putBoolean("module_sort_size_low_to_high", false)
                                .putBoolean("module_sort_size_high_to_low", false)
                                .putBoolean("module_sort_enabled_first", false)
                                .putBoolean("module_sort_action_first", !currentValue)
                                .putBoolean("module_sort_webui_first", false)
                                .commit()
                            moduleViewModel.reloadSortingPreferences(context)
                            scope.launch { moduleViewModel.fetchModuleList() }
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.module_sort_webui_first)) },
                        trailingIcon = {
                            Checkbox(checked = moduleViewModel.sortWebUiFirst, onCheckedChange = null)
                        },
                        onClick = {
                            val currentValue = prefs.getBoolean("module_sort_webui_first", false)
                            prefs.edit()
                                .putBoolean("module_sort_a_to_z", false)
                                .putBoolean("module_sort_z_to_a", false)
                                .putBoolean("module_sort_size_low_to_high", false)
                                .putBoolean("module_sort_size_high_to_low", false)
                                .putBoolean("module_sort_enabled_first", false)
                                .putBoolean("module_sort_action_first", false)
                                .putBoolean("module_sort_webui_first", !currentValue)
                                .commit()
                            moduleViewModel.reloadSortingPreferences(context)
                            scope.launch { moduleViewModel.fetchModuleList() }
                            showDropdown = false
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuperUserTopBar(superUserViewModel: SuperUserViewModel, navigator: DestinationsNavigator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    
    SearchAppBar(
        modifier = modifier,
        title = { 
            Text(
                text = stringResource(R.string.superuser),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) 
        },
        searchText = superUserViewModel.search,
        onSearchTextChange = { superUserViewModel.search = it },
                        onClearClick = { superUserViewModel.search = "" },
        dropdownContent = {
            var showDropdown by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showDropdown = true },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )
                DropdownMenu(
                    expanded = showDropdown, 
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.clip(MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    offset = DpOffset(0.dp, 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.refresh)) }, 
                        onClick = {
                            scope.launch { superUserViewModel.fetchAppList() }
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (superUserViewModel.showSystemApps) {
                                    stringResource(R.string.hide_system_apps)
                                } else {
                                    stringResource(R.string.show_system_apps)
                                }
                            )
                        }, 
                        onClick = {
                            val currentValue = prefs.getBoolean("show_system_apps", false)
                            superUserViewModel.updateShowSystemApps(!currentValue)
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_profile_template)) }, 
                        onClick = {
                            navigator.navigate(AppProfileTemplateScreenDestination)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    title: String,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    TopAppBar(
        modifier = modifier,
        title = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) 
        },
        navigationIcon = {
            IconButton(
                onClick = { navigator.navigateUp() }
            ) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) 
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorTopBar(
    navigator: DestinationsNavigator,
    onSave: () -> Unit = {},
    screenRotationLocked: Boolean = false,
    onScreenRotationToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    TopAppBar(
        title = { 
            Text(
                text = "Photo Editor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        },
        navigationIcon = {
            IconButton(onClick = { navigator.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Removed duplicate icons - using only on-screen buttons
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegularTopBar(
    currentDestination: NavDestination?,
    navigator: DestinationsNavigator,
    flashViewModel: FlashViewModel,
    selectedIconType: IconType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    // Get the selected app name from preferences for home screen title
    val selectedAppName = prefs.getString("selected_app_name", "wild_ksu") ?: "wild_ksu"
    val homeTitle = when (selectedAppName) {
        "kernelsu_next" -> stringResource(R.string.app_name_kernelsu_next)
        "wild_ksu" -> stringResource(R.string.app_name_wild_ksu)
        else -> stringResource(R.string.app_name_kernelsu_next)
    }
    
    // Determine if we need a back button and the title based on current destination
    val titleAndBackButton = when (currentDestination?.route) {
        HomeScreenDestination.route -> homeTitle to false
        SettingScreenDestination.route -> stringResource(R.string.settings) to false
        CustomizationScreenDestination.route -> stringResource(R.string.customization) to true
        DeveloperScreenDestination.route -> stringResource(R.string.developer) to true
        BackupRestoreScreenDestination.route -> stringResource(R.string.backup_restore) to true
        AppProfileScreenDestination.route -> stringResource(R.string.profile) to true
        TemplateEditorScreenDestination.route -> stringResource(R.string.app_profile_template_edit) to true
        AppProfileTemplateScreenDestination.route -> stringResource(R.string.settings_profile_template) to true
        InstallScreenDestination.route -> stringResource(R.string.install) to true
        HomeSettingsScreenDestination.route -> stringResource(R.string.info_card_customization) to true
        PhotoEditorScreenDestination.route -> "Photo Editor" to true
        FlashScreenDestination.route -> {
            val title = when (flashViewModel.flashingStatus) {
                FlashingStatus.WAITING -> "Waiting"
                FlashingStatus.FLASHING -> "Flashing"
                FlashingStatus.SUCCESS -> "Flash Success"
                FlashingStatus.FAILED -> "Flash Failed"
            }
            title to true
        }
        else -> "" to false
    }
    val title = titleAndBackButton.first
    val showBackButton = titleAndBackButton.second
    
    // Animation state for the icon (only for home screen)
    val isHomeScreen = currentDestination?.route == HomeScreenDestination.route
    var rotationState by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(durationMillis = 1000),
        label = "rotation"
    )
    
    // Auto-rotate on initial composition for home screen
    LaunchedEffect(isHomeScreen) {
        if (isHomeScreen) {
            rotationState = 360f
        }
    }
    
    TopAppBar(
        modifier = modifier,
        title = { 
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Show icon to the left of title only on home screen and if not OFF
                if (isHomeScreen && selectedIconType != IconType.OFF) {
                    IconButton(
                        onClick = { 
                            rotationState += 360f
                        }
                    ) {
                        val icon = getIcon(selectedIconType)
                        when (icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = "Icon",
                                modifier = Modifier.graphicsLayer(rotationZ = rotation)
                            )
                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = "Icon",
                                modifier = Modifier.graphicsLayer(rotationZ = rotation)
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (currentDestination?.route == FlashScreenDestination.route) {
                        when (flashViewModel.flashingStatus) {
                            FlashingStatus.WAITING -> Color(0xFFFFEB3B) // Yellow
                            FlashingStatus.FLASHING -> Color(0xFFFF9800) // Orange
                            FlashingStatus.SUCCESS -> Color(0xFF4CAF50) // Green
                            FlashingStatus.FAILED -> Color(0xFFF44336) // Red
                        }
                    } else {
                        LocalContentColor.current
                    }
                )
            }
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
        actions = {
            // Show settings icon when bottom bar is hidden
            val hideBottomBar = prefs.getBoolean("hide_bottom_bar", false)
            if (hideBottomBar && currentDestination?.route == HomeScreenDestination.route) {
                IconButton(
                    onClick = {
                        navigator.navigate(SettingScreenDestination)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
            

            
            // Show LKM and restart icons only on home screen
            if (isHomeScreen) {
                // Bake LKM icon - only show on GKI2 devices (5.10+ kernels)
                val kernelVersion = getKernelVersion()
                if (kernelVersion.isGKI()) {
                    IconButton(
                        onClick = {
                            // Navigate to Install screen for LKM selection and options
                            navigator.navigate(InstallScreenDestination)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = "Bake LKM"
                        )
                    }
                }
                
                // Restart icon with dropdown menu
                var showRestartMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = {
                            showRestartMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Restart"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showRestartMenu,
                        onDismissRequest = { showRestartMenu = false },
                        offset = DpOffset(0.dp, 16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f)
                    ) {
                        // Normal reboot
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot)) },
                            onClick = {
                                showRestartMenu = false
                                reboot()
                            }
                        )
                        
                        // Soft reboot (userspace)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot_userspace)) },
                            onClick = {
                                showRestartMenu = false
                                reboot("userspace")
                            }
                        )
                        
                        // Recovery
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot_recovery)) },
                            onClick = {
                                showRestartMenu = false
                                reboot("recovery")
                            }
                        )
                        
                        // Bootloader
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot_bootloader)) },
                            onClick = {
                                showRestartMenu = false
                                reboot("bootloader")
                            }
                        )
                        
                        // Download mode
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot_download)) },
                            onClick = {
                                showRestartMenu = false
                                reboot("download")
                            }
                        )
                        
                        // EDL mode
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reboot_edl)) },
                            onClick = {
                                showRestartMenu = false
                                reboot("edl")
                            }
                        )
                    }
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}
