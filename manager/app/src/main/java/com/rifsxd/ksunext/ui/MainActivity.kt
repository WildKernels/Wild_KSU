package com.rifsxd.ksunext.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.stringResource
import com.rifsxd.ksunext.ui.screen.BottomBarDestination
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ui.screen.FlashIt
import com.rifsxd.ksunext.ui.theme.AppTheme
import com.rifsxd.ksunext.ui.theme.KernelSUTheme
import com.rifsxd.ksunext.ui.theme.PRIMARY
import com.rifsxd.ksunext.ui.util.*
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.rifsxd.ksunext.ui.webui.WebUIActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    var zipUri by mutableStateOf<ArrayList<Uri>?>(null)
    var navigateLoc by mutableStateOf("")
    var moduleActionId by mutableStateOf<String?>(null)
    var appThemeState = mutableStateOf(AppTheme.AUTO)
    var appThemeCustomColorState = mutableIntStateOf(0xFF8AADF4.toInt())
    private val handler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        try {
            val prefsInit = getSharedPreferences("settings", MODE_PRIVATE)
            val themeValue = prefsInit.getInt("app_theme", 0)
            appThemeState.value = AppTheme.fromValue(themeValue)
            appThemeCustomColorState.intValue = prefsInit.getInt("theme_custom_color", PRIMARY.toArgb())
        } catch (_: Exception) {}

        try {
            LauncherIconManager.applySaved(this)
        } catch (_: Exception) {}

        val isManager = Natives.isManager
        if (isManager) install()

        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            intent.extras?.clear()
            intent = null
        }

        if(intent != null)
            handleIntent(intent)

        setContent {
            KernelSUTheme(
                appTheme = appThemeState.value,
                customColor = Color(appThemeCustomColorState.intValue)
            ) {
                val navController = rememberNavController()
                val snackBarHostState = remember { SnackbarHostState() }
                val navigator = navController.rememberDestinationsNavigator()
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember {
                    context.getSharedPreferences("settings", MODE_PRIVATE)
                }

                var backgroundSettings by remember {
                    mutableStateOf(
                        BackgroundSettings(
                            uri = prefs.getString("background_uri", null),
                            fillScreen = true, // Force zoom to fill
                            isVideo = prefs.getBoolean("background_is_video", false),
                        )
                    )
                }

                var uiOverlaySettings by remember {
                    mutableStateOf(
                        UiOverlaySettings(
                            cardAlpha = run {
                                val transparencyPercent = if (prefs.contains("ui_card_transparency")) {
                                    prefs.getInt("ui_card_transparency", 0)
                                } else {
                                    100 - prefs.getInt("ui_card_alpha", 100)
                                }
                                (1f - (transparencyPercent.coerceIn(0, 100) / 100f))
                            },
                            dimAlpha = prefs.getInt("background_dim", 0) / 100f,
                        )
                    )
                }

                var enableBottomBar by remember {
                    mutableStateOf(prefs.getBoolean("enable_bottom_bar", false))
                }

                DisposableEffect(prefs) {
                    val listener =
                        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                            if (key == "app_theme") {
                                val themeValue = prefs.getInt("app_theme", 0)
                                appThemeState.value = AppTheme.fromValue(themeValue)
                            }
                            if (key == "theme_custom_color") {
                                appThemeCustomColorState.intValue = prefs.getInt("theme_custom_color", PRIMARY.toArgb())
                            }
                            if (key == "background_uri" || key == "background_fill_screen") {
                                backgroundSettings = BackgroundSettings(
                                    uri = prefs.getString("background_uri", null),
                                    fillScreen = true,
                                    isVideo = prefs.getBoolean("background_is_video", false),
                                )
                            }
                            if (key == "background_is_video") {
                                backgroundSettings = BackgroundSettings(
                                    uri = prefs.getString("background_uri", null),
                                    fillScreen = true,
                                    isVideo = prefs.getBoolean("background_is_video", false),
                                )
                            }
                            if (key == "ui_card_alpha" || key == "ui_card_transparency" || key == "background_dim") {
                                uiOverlaySettings = UiOverlaySettings(
                                    cardAlpha = run {
                                        val transparencyPercent = if (prefs.contains("ui_card_transparency")) {
                                            prefs.getInt("ui_card_transparency", 0)
                                        } else {
                                            100 - prefs.getInt("ui_card_alpha", 100)
                                        }
                                        (1f - (transparencyPercent.coerceIn(0, 100) / 100f))
                                    },
                                    dimAlpha = prefs.getInt("background_dim", 0) / 100f,
                                )
                            }
                            if (key == "enable_bottom_bar") {
                                enableBottomBar = prefs.getBoolean("enable_bottom_bar", false)
                            }
                        }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                LaunchedEffect(zipUri, navigateLoc, moduleActionId) {
                    if (moduleActionId != null) {
                        navigator.navigate(ExecuteModuleActionScreenDestination(moduleActionId!!))
                        moduleActionId = null
                    }

                    if (!zipUri.isNullOrEmpty()) {
                        navigator.navigate(
                            FlashScreenDestination(
                                flashIt = FlashIt.FlashModules(zipUri!!)
                            )
                        )
                        zipUri = null
                    }

                    if(zipUri.isNullOrEmpty() && navigateLoc != "")
                    {
                        when(navigateLoc) {
                            "superuser" -> {
                                navigator.navigate(SuperUserScreenDestination)
                            }
                            "modules" -> {
                                navigator.navigate(ModuleScreenDestination)
                            }
                            "settings" -> {
                                navigator.navigate(SettingScreenDestination)
                            }
                        }
                        navigateLoc = ""
                    }
                }

                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                    LocalBackgroundSettings provides backgroundSettings,
                    LocalUiOverlaySettings provides uiOverlaySettings,
                    LocalEnableBottomBar provides enableBottomBar,
                ) {
                    val baseScheme = MaterialTheme.colorScheme
                    val cardAlpha = uiOverlaySettings.cardAlpha.coerceIn(0f, 1f)
                    val scheme = remember(baseScheme, cardAlpha) {
                        baseScheme.copy(
                            surface = baseScheme.surface.copy(alpha = cardAlpha),
                            surfaceVariant = baseScheme.surfaceVariant.copy(alpha = cardAlpha),
                            surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = cardAlpha),
                            surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = cardAlpha),
                            surfaceContainer = baseScheme.surfaceContainer.copy(alpha = cardAlpha),
                            surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = cardAlpha),
                            surfaceContainerHighest = baseScheme.surfaceContainerHighest.copy(alpha = cardAlpha),
                        )
                    }

                    CompositionLocalProvider(
                        LocalBaseColorScheme provides baseScheme,
                    ) {
                        MaterialTheme(
                        colorScheme = scheme,
                        typography = MaterialTheme.typography,
                        shapes = MaterialTheme.shapes,
                        ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppBackground(modifier = Modifier.fillMaxSize())

                            Scaffold(
                                containerColor = Color.Transparent,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    if (enableBottomBar) {
                                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                                        val currentDestination = navBackStackEntry?.destination

                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ) {
                                            BottomBarDestination.values().forEach { destination ->
                                                val isSelected = currentDestination?.route == destination.direction.route ||
                                                        (destination == BottomBarDestination.Settings && currentDestination?.route == CustomizationScreenDestination.route)

                                                if (destination.rootRequired && !Natives.isManager) return@forEach

                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (isSelected) return@NavigationBarItem
                                                        navigator.navigate(destination.direction) {
                                                        launchSingleTop = true
                                                        popUpTo(HomeScreenDestination)
                                                    }
                                                    },
                                                    icon = {
                                                        Icon(
                                                            if (isSelected) destination.iconSelected else destination.iconNotSelected,
                                                            contentDescription = stringResource(destination.label)
                                                        )
                                                    },
                                                    label = { Text(stringResource(destination.label)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                DestinationsNavHost(
                                    modifier = Modifier
                                        .padding(innerPadding)
                                        .let {
                                            if (enableBottomBar) {
                                                it.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                                            } else {
                                                it.windowInsetsPadding(WindowInsets.navigationBars)
                                            }
                                        },
                                    navGraph = NavGraphs.root,
                                    navController = navController,
                                    defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                                        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                            {
                                                fadeIn(animationSpec = tween(250)) + scaleIn(
                                                    initialScale = 0.92f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                )
                                            }

                                        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                            {
                                                fadeOut(animationSpec = tween(250)) + scaleOut(
                                                    targetScale = 0.92f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                )
                                            }

                                        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                            {
                                                fadeIn(animationSpec = tween(250)) + scaleIn(
                                                    initialScale = 0.92f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                )
                                            }

                                        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                            {
                                                fadeOut(animationSpec = tween(250)) + scaleOut(
                                                    targetScale = 0.92f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                )
                                            }
                                    }
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_amoled", enabled).apply()
            
            val newTheme = if (enabled) AppTheme.AMOLED else AppTheme.AUTO
            prefs.edit().putInt("app_theme", newTheme.value).apply()
            appThemeState.value = newTheme
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val shortcutType = intent.getStringExtra("shortcut_type")
        if (shortcutType == "module_action") {
            moduleActionId = intent.getStringExtra("module_id")
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                zipUri =
                    intent.data?.let { arrayListOf(it) }
                        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra("uris", Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayListExtra("uris")
                        }
            }

            "ACTION_SETTINGS" -> navigateLoc = "settings"
            "ACTION_SUPERUSER" -> navigateLoc = "superuser"
            "ACTION_MODULES" -> navigateLoc = "modules"
        }
    }
}

@Composable
private fun AppBackground(modifier: Modifier = Modifier) {
    val backgroundSettings = LocalBackgroundSettings.current
    val uiOverlaySettings = LocalUiOverlaySettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val baseColor = MaterialTheme.colorScheme.background

    val contentScale = if (backgroundSettings.fillScreen) {
        ContentScale.Crop
    } else {
        ContentScale.Fit
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor)
        )

    val uri = backgroundSettings.uri
    if (uri != null) {
            if (backgroundSettings.isVideo) {
                VideoBackground(
                    uri = uri,
                    fillScreen = backgroundSettings.fillScreen,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(android.net.Uri.parse(uri))
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }

            val dimAlpha = uiOverlaySettings.dimAlpha.coerceIn(0f, 1f)
            if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                )
            }
        }
    }
}

@Composable
private fun VideoBackground(
    uri: String,
    fillScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = true
                setMediaItem(MediaItem.fromUri(android.net.Uri.parse(uri)))
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = exoPlayer
                resizeMode = if (fillScreen) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        },
        update = { view ->
            view.resizeMode = if (fillScreen) {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    )
}
