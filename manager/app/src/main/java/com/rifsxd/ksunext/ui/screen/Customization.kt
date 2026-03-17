package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewCarousel
import android.widget.Toast
import com.rifsxd.ksunext.ui.util.SettingsBackupHelper
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import com.rifsxd.ksunext.ui.MainActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.*
import com.rifsxd.ksunext.ui.util.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.rifsxd.ksunext.ui.theme.AppTheme
import com.rifsxd.ksunext.ui.theme.ColorPickerDialog
import com.rifsxd.ksunext.ui.theme.KernelSUTheme
import com.rifsxd.ksunext.ui.theme.PRIMARY
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import android.webkit.MimeTypeMap
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * @author rifsxd
 * @date 2025/6/1.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                    if (!navigator.popBackStack()) {
                        navigator.navigate(SettingScreenDestination)
                    }
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
            val density = LocalDensity.current

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

                    SelectionDialog(
                        title = stringResource(R.string.settings_language),
                        options = allOptions,
                        selectedOption = currentLocale,
                        onOptionSelected = { newLocale ->
                            prefs.edit { putString("app_locale", newLocale) }
                            currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
                            refreshActivity(context)
                            dismiss()
                        },
                        onDismissRequest = { dismiss() }
                    )
                }
            }

            var backgroundUri by rememberSaveable {
                mutableStateOf(prefs.getString("background_uri", null))
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

            var backgroundDimPercent by rememberSaveable {
                mutableStateOf(prefs.getInt("background_dim", 0))
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

            var useBanner by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("use_banner", true)
                )
            }

            var enableBottomBar by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_bottom_bar", false)
                )
            }

            var enableAmoled by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_amoled", false)
                )
            }

            val cardAlpha = LocalUiOverlaySettings.current.cardAlpha
            val elevatedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow

            // Card 1: Interface
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = elevatedContainerColor),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                languageDialog.show()
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (ksuVersion != null) {
                        SwitchItem(
                            icon = Icons.Filled.ViewCarousel,
                            title = stringResource(id = R.string.settings_banner),
                            summary = stringResource(id = R.string.settings_banner_summary),
                            checked = useBanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            prefs.edit { putBoolean("use_banner", it) }
                            useBanner = it
                        }
                    }

                    SwitchItem(
                        icon = Icons.Filled.ViewStream,
                        title = stringResource(id = R.string.settings_enable_bottom_bar),
                        summary = stringResource(id = R.string.settings_enable_bottom_bar_summary),
                        checked = enableBottomBar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        prefs.edit { putBoolean("enable_bottom_bar", it) }
                        enableBottomBar = it
                    }
                }
            }

            // Card 2: Theming
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = elevatedContainerColor),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                stringResource(R.string.settings_background_selected)
                            }
                            Text(subtitle)
                        },
                        trailingContent = {
                            if (backgroundUri != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                backgroundPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (backgroundUri != null) {
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (isSystemInDarkTheme()) {
                        // Keep this block for layout consistency if needed, but removing old AMOLED switch logic from here
                    }

                    // --- Theme Selector ---
                    val currentThemeValue = prefs.getInt("app_theme", AppTheme.AUTO.value)
                    val currentTheme = AppTheme.fromValue(currentThemeValue)
                    val themeOptions = listOf(
                        AppTheme.AUTO to "Auto",
                        AppTheme.DARK_DYNAMIC to "Dark Dynamic",
                        AppTheme.LIGHT_DYNAMIC to "Light Dynamic",
                        AppTheme.LIGHT to "Light",
                        AppTheme.DARK to "Dark",
                        AppTheme.AMOLED to "AMOLED",
                        AppTheme.CUSTOM to "Custom"
                    )
                    
                    var showThemeDialog by remember { mutableStateOf(false) }
                    
                    ListItem(
                        headlineContent = { Text(text = "App Theme") },
                        supportingContent = { Text(text = themeOptions.find { it.first == currentTheme }?.second ?: "Auto") },
                        leadingContent = { Icon(Icons.Filled.Contrast, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                showThemeDialog = true
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    if (showThemeDialog) {
                        SelectionDialog(
                            title = "Select Theme",
                            options = themeOptions,
                            selectedOption = currentTheme,
                            onOptionSelected = { selectedTheme ->
                                prefs.edit { putInt("app_theme", selectedTheme.value) }
                                prefs.edit { putBoolean("enable_amoled", selectedTheme == AppTheme.AMOLED) }
                                showThemeDialog = false
                            },
                            onDismissRequest = { showThemeDialog = false }
                        )
                    }

                    var currentLauncherIcon by remember {
                        mutableStateOf(LauncherIconManager.getSelected(context))
                    }
                    var showLauncherIconDialog by remember { mutableStateOf(false) }

                    val selectedIconBitmap = remember(currentLauncherIcon, density) {
                        val sizePx = with(density) { 24.dp.roundToPx() }
                        LauncherIconManager.loadPreviewBitmap(context, currentLauncherIcon, sizePx)
                    }

                    ListItem(
                        headlineContent = { Text(text = "App Icon") },
                        supportingContent = { Text(text = currentLauncherIcon.label) },
                        leadingContent = { Icon(Icons.Filled.Apps, contentDescription = null) },
                        trailingContent = selectedIconBitmap?.let { bitmap ->
                            {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showLauncherIconDialog = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (showLauncherIconDialog) {
                        var selection by remember(currentLauncherIcon) { mutableStateOf(currentLauncherIcon) }

                        BlurDialog(onDismissRequest = { showLauncherIconDialog = false }) {
                            Text(
                                text = "Select App Icon",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 4,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                LauncherIcon.entries.forEach { icon ->
                                    val selected = icon == selection
                                    val previewBitmap = remember(icon, density) {
                                        val sizePx = with(density) { 56.dp.roundToPx() }
                                        LauncherIconManager.loadPreviewBitmap(context, icon, sizePx)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .widthIn(min = 72.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { selection = icon }
                                            .padding(horizontal = 10.dp, vertical = 12.dp)
                                    ) {
                                        if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(56.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = icon.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showLauncherIconDialog = false }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    try {
                                        LauncherIconManager.setSelected(context, selection)
                                        currentLauncherIcon = selection
                                        scope.launch {
                                            snackBarHost.showSnackbar("App icon updated. If it doesn't change, restart your launcher.")
                                        }
                                    } catch (_: Exception) {
                                        scope.launch {
                                            snackBarHost.showSnackbar("Failed to change app icon.")
                                        }
                                    } finally {
                                        showLauncherIconDialog = false
                                    }
                                }) {
                                    Text(stringResource(android.R.string.ok))
                                }
                            }
                        }
                    }

                    if (currentTheme == AppTheme.CUSTOM) {
                        var currentCustomColor by remember { mutableIntStateOf(prefs.getInt("theme_custom_color", PRIMARY.toArgb())) }
                        var showColorPicker by remember { mutableStateOf(false) }

                        if (showColorPicker) {
                            ColorPickerDialog(
                                initialColor = Color(currentCustomColor),
                                onDismissRequest = { showColorPicker = false },
                                onColorSelected = { color ->
                                    val colorInt = color.toArgb()
                                    prefs.edit { putInt("theme_custom_color", colorInt) }
                                    currentCustomColor = colorInt
                                    showColorPicker = false
                                    refreshActivity(context)
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Custom Color") },
                            supportingContent = { Text("Tap to pick a color") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(currentCustomColor))
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showColorPicker = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        // Custom Theme Mode (Light/Dark/AMOLED)
                        var currentThemeMode by remember { mutableStateOf(prefs.getString("theme_custom_base_mode", "light") ?: "light") }
                        val themeModeOptions = listOf(
                            "light" to "Light",
                            "dark" to "Dark",
                            "amoled" to "AMOLED"
                        )
                        var showThemeModeDialog by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = { Text("Theme Mode") },
                            supportingContent = { Text(themeModeOptions.find { it.first == currentThemeMode }?.second ?: "Light") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeModeDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (showThemeModeDialog) {
                            SelectionDialog(
                                title = "Select Theme Mode",
                                options = themeModeOptions,
                                selectedOption = currentThemeMode,
                                onOptionSelected = { selectedMode ->
                                    prefs.edit { putString("theme_custom_base_mode", selectedMode) }
                                    currentThemeMode = selectedMode
                                    refreshActivity(context) // Force activity refresh to apply theme mode change
                                    showThemeModeDialog = false
                                },
                                onDismissRequest = { showThemeModeDialog = false }
                            )
                        }

                        // Custom Text Color
                        var customTextColor by remember { mutableIntStateOf(prefs.getInt("theme_custom_text_color", 0)) } // 0 means default/not set
                        var showTextColorPicker by remember { mutableStateOf(false) }

                        if (showTextColorPicker) {
                            ColorPickerDialog(
                                initialColor = if (customTextColor != 0) Color(customTextColor) else MaterialTheme.colorScheme.onSurface,
                                onDismissRequest = { showTextColorPicker = false },
                                onColorSelected = { color ->
                                    val colorInt = color.toArgb()
                                    prefs.edit { putInt("theme_custom_text_color", colorInt) }
                                    customTextColor = colorInt
                                    showTextColorPicker = false
                                    refreshActivity(context)
                                }
                            )
                        }

                        ListItem(
                            headlineContent = { Text("Text Color") },
                            supportingContent = { Text("Tap to pick text color") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (customTextColor != 0) Color(customTextColor) else MaterialTheme.colorScheme.onSurface)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTextColorPicker = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        // Reset Button
                        Button(
                            onClick = {
                                prefs.edit {
                                    remove("theme_custom_color")
                                    remove("theme_custom_base_mode")
                                    remove("theme_custom_text_color")
                                }
                                refreshActivity(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reset Custom Theme")
                        }
                    }
                }
            }
            // Card 3: Info Card Customization
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = elevatedContainerColor),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Info Card Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    var infoCardAlwaysExpanded by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("info_card_always_expanded", false))
                    }

                    SwitchItem(
                        icon = Icons.Filled.UnfoldMore,
                        title = "Always Expanded",
                        summary = "Keep the Info Card expanded by default",
                        checked = infoCardAlwaysExpanded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        prefs.edit { putBoolean("info_card_always_expanded", it) }
                        infoCardAlwaysExpanded = it
                    }

                    var modulesAlwaysExpanded by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("modules_always_expanded", false))
                    }

                    SwitchItem(
                        icon = Icons.Filled.ViewStream,
                        title = "Always Expand Modules",
                        summary = "Keep all module cards expanded by default",
                        checked = modulesAlwaysExpanded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        prefs.edit { putBoolean("modules_always_expanded", it) }
                        modulesAlwaysExpanded = it
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    var infoCardItems by remember { mutableStateOf(InfoCardHelper.getConfig(context)) }

                    infoCardItems.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(stringResource(InfoCardHelper.getLabelResId(item.id))) },
                            leadingContent = {
                                Checkbox(
                                    checked = item.visible,
                                    onCheckedChange = { checked ->
                                        val newItems = infoCardItems.toMutableList()
                                        newItems[index] = item.copy(visible = checked)
                                        infoCardItems = newItems
                                        InfoCardHelper.saveConfig(context, newItems)
                                    }
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newItems = infoCardItems.toMutableList()
                                                val temp = newItems[index]
                                                newItems[index] = newItems[index - 1]
                                                newItems[index - 1] = temp
                                                infoCardItems = newItems
                                                InfoCardHelper.saveConfig(context, newItems)
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < infoCardItems.size - 1) {
                                                val newItems = infoCardItems.toMutableList()
                                                val temp = newItems[index]
                                                newItems[index] = newItems[index + 1]
                                                newItems[index + 1] = temp
                                                infoCardItems = newItems
                                                InfoCardHelper.saveConfig(context, newItems)
                                            }
                                        },
                                        enabled = index < infoCardItems.size - 1
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
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
