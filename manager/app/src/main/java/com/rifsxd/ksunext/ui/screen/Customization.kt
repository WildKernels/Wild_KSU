package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import com.rifsxd.ksunext.ui.MainActivity
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.util.restartActivity
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.LocaleHelper
import java.io.File
import java.io.FileOutputStream
import android.webkit.MimeTypeMap
import kotlin.math.roundToInt

/**
 * @author rifsxd
 * @date 2025/6/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
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

            // Track language state with current app locale
            var currentAppLocale by remember { mutableStateOf(LocaleHelper.getCurrentAppLocale(context)) }
            
            // Listen for preference changes
            LaunchedEffect(Unit) {
                currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
            }

            // Language setting with selection dialog
            val languageDialog = rememberCustomDialog { dismiss ->
                // Check if should use system language settings
                if (LocaleHelper.useSystemLanguageSettings) {
                    // Android 13+ - Jump to system settings
                    LocaleHelper.launchSystemLanguageSettings(context)
                    dismiss()
                } else {
                    // Android < 13 - Show app language selector
                    // Dynamically detect supported locales from resources
                    val supportedLocales = remember {
                        val locales = mutableListOf<java.util.Locale>()
                        
                        // Add system default first
                        locales.add(java.util.Locale.ROOT) // This will represent "System Default"
                        
                        // Dynamically detect available locales by checking resource directories
                        val resourceDirs = listOf(
                            "ar", "bg", "de", "fa", "fr", "hu", "in", "it", 
                            "ja", "ko", "pl", "pt-rBR", "ru", "th", "tr", 
                            "uk", "vi", "zh-rCN", "zh-rTW"
                        )
                        
                        resourceDirs.forEach { dir ->
                            try {
                                val locale = when {
                                    dir.contains("-r") -> {
                                        val parts = dir.split("-r")
                                        java.util.Locale.Builder()
                                            .setLanguage(parts[0])
                                            .setRegion(parts[1])
                                            .build()
                                    }
                                    else -> java.util.Locale.Builder()
                                        .setLanguage(dir)
                                        .build()
                                }
                                
                                // Test if this locale has translated resources
                                val config = android.content.res.Configuration()
                                config.setLocale(locale)
                                val localizedContext = context.createConfigurationContext(config)
                                
                                // Try to get a translated string to verify the locale is supported
                                val testString = localizedContext.getString(R.string.settings_language)
                                val defaultString = context.getString(R.string.settings_language)
                                
                                // If the string is different or it's English, it's supported
                                if (testString != defaultString || locale.language == "en") {
                                    locales.add(locale)
                                }
                            } catch (_: Exception) {
                                // Skip unsupported locales
                            }
                        }
                        
                        // Sort by display name
                        val sortedLocales = locales.drop(1).sortedBy { it.getDisplayName(it) }
                        mutableListOf<java.util.Locale>().apply {
                            add(locales.first()) // System default first
                            addAll(sortedLocales)
                        }
                    }
                    
                    val allOptions = supportedLocales.map { locale ->
                        val tag = if (locale == java.util.Locale.ROOT) {
                            "system"
                        } else if (locale.country.isEmpty()) {
                            locale.language
                        } else {
                            "${locale.language}_${locale.country}"
                        }
                        
                        val displayName = if (locale == java.util.Locale.ROOT) {
                            context.getString(R.string.system_default)
                        } else {
                            locale.getDisplayName(locale)
                        }
                        
                        tag to displayName
                    }
                    
                    val currentLocale = prefs.getString("app_locale", "system") ?: "system"
                    val options = allOptions.map { (tag, displayName) ->
                        ListOption(
                            titleText = displayName,
                            selected = currentLocale == tag
                        )
                    }
                    
                    var selectedIndex by remember { 
                        mutableIntStateOf(allOptions.indexOfFirst { (tag, _) -> currentLocale == tag })
                    }
                    
                    ListDialog(
                        state = rememberUseCaseState(
                            visible = true,
                            onFinishedRequest = {
                                if (selectedIndex >= 0 && selectedIndex < allOptions.size) {
                                    val newLocale = allOptions[selectedIndex].first
                                    prefs.edit { putString("app_locale", newLocale) }
                                    
                                    // Update local state immediately
                                    currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
                                    
                                    // Apply locale change immediately for Android < 13
                                    LocaleHelper.restartActivity(context)
                                }
                                dismiss()
                            },
                            onCloseRequest = {
                                dismiss()
                            }
                        ),
                        header = Header.Default(
                            title = stringResource(R.string.settings_language),
                        ),
                        selection = ListSelection.Single(
                            showRadioButtons = true,
                            options = options
                        ) { index, _ ->
                            selectedIndex = index
                        }
                    )
                }
            }

            val language = stringResource(id = R.string.settings_language)
            
            // Compute display name based on current app locale (similar to the reference implementation)
            val currentLanguageDisplay = remember(currentAppLocale) {
                val locale = currentAppLocale
                if (locale != null) {
                    locale.getDisplayName(locale)
                } else {
                    context.getString(R.string.system_default)
                }
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.Translate, language) },
                headlineContent = { Text(
                    text = language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { Text(currentLanguageDisplay) },
                modifier = Modifier.clickable {
                    languageDialog.show()
                }
            )

            var backgroundUri by rememberSaveable {
                mutableStateOf(prefs.getString("background_uri", null))
            }
            var backgroundFillScreen by rememberSaveable {
                mutableStateOf(prefs.getBoolean("background_fill_screen", false))
            }
            var backgroundIsVideo by rememberSaveable {
                mutableStateOf(prefs.getBoolean("background_is_video", false))
            }

            val backgroundPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val mime = context.contentResolver.getType(uri)
                val isVideo = mime?.startsWith("video/") == true
                val storedUri = copyBackgroundToAppStorage(context, uri, mime) ?: return@rememberLauncherForActivityResult
                backgroundUri?.let { deleteOwnedBackgroundFile(context, it) }
                prefs.edit {
                    putString("background_uri", storedUri)
                    putBoolean("background_is_video", isVideo)
                }
                backgroundUri = storedUri
                backgroundIsVideo = isVideo
            }

            ListItem(
                leadingContent = {
                    Icon(Icons.Filled.Wallpaper, null)
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_background),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    val subtitle = if (backgroundUri == null) {
                        stringResource(R.string.settings_background_choose_media)
                    } else {
                        val mode = if (backgroundFillScreen) {
                            stringResource(R.string.settings_background_scale_zoom)
                        } else {
                            stringResource(R.string.settings_background_scale_fit)
                        }
                        "${stringResource(R.string.settings_background_selected)} • $mode"
                    }
                    Text(subtitle)
                },
                trailingContent = {
                    if (backgroundUri != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val next = !backgroundFillScreen
                                    prefs.edit {
                                        putBoolean("background_fill_screen", next)
                                    }
                                    backgroundFillScreen = next
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Flip,
                                    contentDescription = stringResource(R.string.settings_background_scale_photo)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val uriString = backgroundUri
                                    if (uriString != null) {
                                        deleteOwnedBackgroundFile(context, uriString)
                                    }
                                    prefs.edit {
                                        remove("background_uri")
                                        remove("background_is_video")
                                    }
                                    backgroundUri = null
                                    backgroundIsVideo = false
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.settings_background_clear)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.clickable {
                    backgroundPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            )

            if (backgroundUri != null) {
                var backgroundDimPercent by rememberSaveable {
                    mutableStateOf(prefs.getInt("background_dim", 0))
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_background_dim),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$backgroundDimPercent%")
                            Slider(
                                value = backgroundDimPercent / 100f,
                                onValueChange = { v ->
                                    val next = (v * 100).roundToInt().coerceIn(0, 100)
                                    backgroundDimPercent = next
                                    prefs.edit {
                                        putInt("background_dim", next)
                                    }
                                }
                            )
                        }
                    }
                )
            }

            var cardTransparencyPercent by rememberSaveable {
                mutableStateOf(
                    if (prefs.contains("ui_card_transparency")) {
                        prefs.getInt("ui_card_transparency", 0)
                    } else {
                        100 - prefs.getInt("ui_card_alpha", 100)
                    }
                )
            }
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_ui_card_transparency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$cardTransparencyPercent%")
                        Slider(
                            value = cardTransparencyPercent / 100f,
                            onValueChange = { v ->
                                val next = (v * 100).roundToInt().coerceIn(0, 100)
                                cardTransparencyPercent = next
                                prefs.edit {
                                    putInt("ui_card_transparency", next)
                                }
                            }
                        )
                    }
                }
            )

            var useBanner by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("use_banner", true)
                )
            }
            if (ksuVersion != null) {
                SwitchItem(
                    icon = Icons.Filled.ViewCarousel,
                    title = stringResource(id = R.string.settings_banner),
                    summary = stringResource(id = R.string.settings_banner_summary),
                    checked = useBanner
                ) {
                    prefs.edit { putBoolean("use_banner", it) }
                    useBanner = it
                }
            }

            var enableBottomBar by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_bottom_bar", false)
                )
            }
            SwitchItem(
                icon = Icons.Filled.ViewStream,
                title = stringResource(id = R.string.settings_enable_bottom_bar),
                summary = stringResource(id = R.string.settings_enable_bottom_bar_summary),
                checked = enableBottomBar
            ) {
                prefs.edit { putBoolean("enable_bottom_bar", it) }
                enableBottomBar = it
            }

            var enableAmoled by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_amoled", false)
                )
            }
            if (isSystemInDarkTheme()) {
                val activity = LocalContext.current as? MainActivity
                SwitchItem(
                    icon = Icons.Filled.Contrast,
                    title = stringResource(id = R.string.settings_amoled_mode),
                    summary = stringResource(id = R.string.settings_amoled_mode_summary),
                    checked = enableAmoled
                ) { checked ->
                    activity?.setAmoledMode(checked)
                    enableAmoled = checked
                }
            }
        }
    }
}

private fun copyBackgroundToAppStorage(context: Context, uri: Uri, mimeType: String?): String? {
    val backgroundsDir = File(context.filesDir, "backgrounds")
    if (!backgroundsDir.exists()) {
        backgroundsDir.mkdirs()
    }

    val ext = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    val dstFile = if (!ext.isNullOrBlank()) {
        File(backgroundsDir, "background_${System.currentTimeMillis()}.$ext")
    } else {
        File(backgroundsDir, "background_${System.currentTimeMillis()}")
    }
    val resolver = context.contentResolver
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(dstFile).use { output ->
            input.copyTo(output)
        }
    } ?: return null

    return Uri.fromFile(dstFile).toString()
}

private fun deleteOwnedBackgroundFile(context: Context, uriString: String) {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
    if (uri.scheme != "file") return
    val path = uri.path ?: return
    val ownedDir = File(context.filesDir, "backgrounds").absolutePath + File.separator
    if (!path.startsWith(ownedDir)) return
    runCatching { File(path).delete() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(
                text = stringResource(R.string.customization),
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
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
