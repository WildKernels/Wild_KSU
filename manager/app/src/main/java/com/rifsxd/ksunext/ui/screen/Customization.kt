package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.lifecycle.compose.dropUnlessResumed
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.InfoCardSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.component.AdvancedImageCropDialog
import com.rifsxd.ksunext.ui.component.IconThemeManagerDialog
import com.rifsxd.ksunext.ui.util.ImageCropUtils
import com.rifsxd.ksunext.ui.util.LocaleHelper
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.util.IconPackHelper
import com.rifsxd.ksunext.ui.util.IconPack

import java.util.Locale

/**
 * @author rifsxd
 * @date 2025/6/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.becomeManager(ksuApp.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {

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
                            } catch (e: Exception) {
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
                                    prefs.edit().putString("app_locale", newLocale).apply()
                                    
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
                modifier = Modifier
                    .clickable {
                        languageDialog.show()
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
                    prefs.edit().putBoolean("use_banner", it).apply()
                    useBanner = it
                }
            }

            // Theme Mode Selection
            var themeMode by rememberSaveable {
                mutableStateOf(
                    prefs.getString("theme_mode", "system_default") ?: "system_default"
                )
            }
            
            val themeOptions = listOf(
                "system_default" to "System Default",
                "light" to "Light Mode",
                "dark" to "Dark Mode",
                "amoled" to "AMOLED Dark"
            )
            
            val currentThemeDisplay = themeOptions.find { it.first == themeMode }?.second ?: "System Default"
            
            val themeDialog = rememberCustomDialog { dismiss ->
                val options = themeOptions.map { (value, display) ->
                    ListOption(
                        titleText = display,
                        selected = value == themeMode
                    )
                }
                
                ListDialog(
                    state = rememberUseCaseState(visible = true, onCloseRequest = { dismiss() }),
                    header = Header.Default(title = "Theme Mode"),
                    selection = ListSelection.Single(
                        showRadioButtons = true,
                        options = options
                    ) { index, _ ->
                        val selectedTheme = themeOptions[index].first
                        prefs.edit().putString("theme_mode", selectedTheme).commit()
                        themeMode = selectedTheme
                        dismiss()
                    }
                )
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.Palette, "Theme Mode") },
                headlineContent = { Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { Text("Current: $currentThemeDisplay") },
                modifier = Modifier
                    .clickable {
                        themeDialog.show()
                    }
            )

            // Background Image Setting
            var backgroundImageUri by rememberSaveable {
                mutableStateOf(
                    prefs.getString("background_image_uri", null)
                )
            }
            
            // State for crop dialog
            var showCropDialog by remember { mutableStateOf(false) }
            var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

            val selectImageLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        // Take persistable permission for the selected image
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            selectedImageUri = uri
                            showCropDialog = true
                        } catch (e: Exception) {
                            // Handle permission error - fallback to direct save
                            e.printStackTrace()
                            prefs.edit().putString("background_image_uri", uri.toString()).apply()
                            backgroundImageUri = uri.toString()
                        }
                    }
                }
            }

            // Advanced image crop dialog
            if (showCropDialog && selectedImageUri != null) {
                AdvancedImageCropDialog(
                    imageUri = selectedImageUri!!,
                    onDismiss = {
                        showCropDialog = false
                        selectedImageUri = null
                    },
                    onSave = {
                        // Save the image URI
                        prefs.edit().apply {
                            putString("background_image_uri", selectedImageUri.toString())
                            apply()
                        }
                        backgroundImageUri = selectedImageUri.toString()
                        showCropDialog = false
                        selectedImageUri = null
                    }
                )
            }

            // Background Image Selection
            ListItem(
                leadingContent = { Icon(Icons.Filled.Image, stringResource(R.string.settings_background_image)) },
                headlineContent = { Text(
                    text = stringResource(R.string.settings_background_image),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { Text(stringResource(R.string.settings_background_image_summary)) },
                trailingContent = {
                    Row {
                        // Crop button (only show if background image is selected)
                        if (backgroundImageUri != null) {
                            IconButton(onClick = {
                                // Open crop dialog for current image
                                backgroundImageUri?.let { uriString ->
                                    selectedImageUri = Uri.parse(uriString)
                                    showCropDialog = true
                                }
                            }) {
                                Icon(Icons.Filled.Crop, stringResource(R.string.crop_background_image))
                            }
                        }
                        // Delete button (only show if background image is selected)
                        if (backgroundImageUri != null) {
                            IconButton(onClick = {
                                prefs.edit().remove("background_image_uri").commit()
                                backgroundImageUri = null
                            }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.background_image_remove))
                            }
                        }
                    }
                },
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        selectImageLauncher.launch(intent)
                    }
            )

            // Background Transparency Slider (Darkness) - Only show when background image is enabled
            if (backgroundImageUri != null) {
                var backgroundTransparency by rememberSaveable {
                    mutableFloatStateOf(
                        prefs.getFloat("background_transparency", 1.0f)
                    )
                }
                
                ListItem(
                leadingContent = { Icon(Icons.Filled.Opacity, stringResource(R.string.background_transparency)) },
                headlineContent = { Text(
                    text = stringResource(R.string.background_transparency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { 
                    Column {
                        Text(stringResource(R.string.background_transparency_summary))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp)
                            )
                            Slider(
                                value = backgroundTransparency,
                                onValueChange = { value ->
                                    backgroundTransparency = value
                                    prefs.edit().putFloat("background_transparency", value).commit()
                                },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors()
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = "${(backgroundTransparency * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Min/Low/Med/High/Max preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(
                                0.0f to "Min",
                                0.25f to "Low", 
                                0.5f to "Med",
                                0.75f to "High",
                                1.0f to "Max"
                            )
                            
                            presets.forEach { (value, label) ->
                                Button(
                                    onClick = { 
                                        backgroundTransparency = value
                                        prefs.edit().putFloat("background_transparency", value).commit()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (backgroundTransparency == value) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (backgroundTransparency == value) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                )

                // Background Blur Slider
                var backgroundBlur by rememberSaveable {
                    mutableFloatStateOf(
                        prefs.getFloat("background_blur", 0.0f)
                    )
                }
                
                ListItem(
                leadingContent = { Icon(Icons.Filled.BlurCircular, "Background Blur") },
                headlineContent = { Text(
                    text = "Background Blur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { 
                    Column {
                        Text("Add blur effect to background image")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp)
                            )
                            Slider(
                                value = backgroundBlur,
                                onValueChange = { value ->
                                    backgroundBlur = value
                                    prefs.edit().putFloat("background_blur", value).commit()
                                },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors()
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = "${(backgroundBlur * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Min/Low/Med/High/Max preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(
                                0.0f to "Min",
                                0.25f to "Low", 
                                0.5f to "Med",
                                0.75f to "High",
                                1.0f to "Max"
                            )
                            
                            presets.forEach { (value, label) ->
                                Button(
                                    onClick = { 
                                        backgroundBlur = value
                                        prefs.edit().putFloat("background_blur", value).commit()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (backgroundBlur == value) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (backgroundBlur == value) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                )
            }

            // UI Transparency Slider - Always available
            var uiTransparency by rememberSaveable {
                mutableFloatStateOf(
                    prefs.getFloat("ui_transparency", 0.0f)
                )
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.Tune, "UI Transparency") },
                headlineContent = { Text(
                    text = "UI Transparency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { 
                    Column {
                        Text("Adjust the transparency of UI elements")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp)
                            )
                            Slider(
                                value = uiTransparency,
                                onValueChange = { value ->
                                    uiTransparency = value
                                    prefs.edit().putFloat("ui_transparency", value).commit()
                                },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors()
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = "${(uiTransparency * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Min/Low/Med/High/Max preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(
                                0.0f to "Min",
                                0.25f to "Low", 
                                0.5f to "Med",
                                0.75f to "High",
                                1.0f to "Max"
                            )
                            
                            presets.forEach { (value, label) ->
                                Button(
                                    onClick = { 
                                        uiTransparency = value
                                        prefs.edit().putFloat("ui_transparency", value).commit()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiTransparency == value) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (uiTransparency == value) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // DPI Scale Settings
            val systemDpi = remember { context.resources.displayMetrics.densityDpi }
            val savedDpi = remember { 
                if (prefs.contains("app_dpi")) {
                    prefs.getInt("app_dpi", systemDpi)
                } else {
                    systemDpi // Use system DPI when no custom setting exists
                }
            }
            var currentDpi by rememberSaveable { 
                mutableIntStateOf(savedDpi)
            }
            var showDpiConfirmDialog by remember { mutableStateOf(false) }
            
            // DPI confirmation dialog
            if (showDpiConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDpiConfirmDialog = false },
                    title = { Text(stringResource(R.string.dpi_confirm_title)) },
                    text = { 
                        Column {
                            Text(stringResource(R.string.dpi_confirm_message))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.dpi_confirm_summary, currentDpi, savedDpi),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Apply DPI changes
                                prefs.edit().putInt("app_dpi", currentDpi).apply()
                                
                                // Calculate scale factor for MainActivity
                                val scale = currentDpi.toFloat() / systemDpi.toFloat()
                                prefs.edit().putFloat("dpi_scale", scale).apply()
                                
                                showDpiConfirmDialog = false
                                
                                // Restart activity to apply changes
                                (context as? Activity)?.recreate()
                            }
                        ) {
                            Text(
                                stringResource(R.string.dpi_apply_settings),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 14.sp
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDpiConfirmDialog = false }
                        ) {
                            Text(
                                stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.ZoomIn, stringResource(R.string.app_dpi_title)) },
                headlineContent = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_dpi_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        // Reset button
                        TextButton(
                            onClick = { 
                                // Reset to system DPI - this will trigger the apply button to show
                                currentDpi = systemDpi
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Reset", 
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                supportingContent = { 
                    Column {
                        Text(stringResource(R.string.app_dpi_summary))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current and System DPI info
                        Text(
                            text = stringResource(R.string.dpi_current, currentDpi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.dpi_system, systemDpi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // DPI Slider (removed numbers)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = currentDpi.toFloat(),
                                onValueChange = { value ->
                                    currentDpi = value.toInt()
                                },
                                valueRange = 160f..600f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors()
                            )
                        }
                        
                        Text(
                            text = "$currentDpi DPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Min/Low/Med/High/Max preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val presets = listOf(
                                160 to "Min",
                                240 to "Low", 
                                320 to "Med",
                                400 to "High",
                                600 to "Max"
                            )
                            
                            presets.forEach { (dpi, label) ->
                                Button(
                                    onClick = { currentDpi = dpi },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentDpi == dpi) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (currentDpi == dpi) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        // Apply button (only show when changes are made)
                        if (currentDpi != savedDpi) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDpiConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.dpi_apply_settings),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            )

            // Help Card Customization
            var showHelpCard by rememberSaveable {
                mutableStateOf(prefs.getBoolean("show_help_card", true))
            }

            // System Info Card Settings
            ListItem(
                leadingContent = { 
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "System Info Card Settings"
                    ) 
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.info_card_customization),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.info_card_customization_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate to settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { 
                    navigator.navigate(InfoCardSettingsScreenDestination)
                }
            )

            // Help Card Customization
            ListItem(
                leadingContent = { 
                    Icon(
                        imageVector = Icons.Filled.Help,
                        contentDescription = "Help Card Settings"
                    ) 
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.help_card_customization),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.help_card_customization_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showHelpCard,
                        onCheckedChange = {
                            prefs.edit().putBoolean("show_help_card", it).apply()
                            showHelpCard = it
                        }
                    )
                },
                modifier = Modifier.clickable { 
                    val newValue = !showHelpCard
                    prefs.edit().putBoolean("show_help_card", newValue).apply()
                    showHelpCard = newValue
                }
            )

            // Home Icon Customization
            var selectedIconType by rememberSaveable {
                mutableStateOf(
                    prefs.getString("selected_icon_type", "SEASONAL") ?: "SEASONAL"
                )
            }
            
            // Icon Selection with OFF option
            val iconOptions = listOf(
                "OFF" to "Off",
                "SEASONAL" to "Seasonal (Auto)",
                "WINTER" to "Winter",
                "SPRING" to "Spring", 
                "SUMMER" to "Summer",
                "FALL" to "Fall"
            )
            
            val currentIconDisplay = iconOptions.find { it.first == selectedIconType }?.second ?: "Seasonal (Auto)"
            
            val iconDialog = rememberCustomDialog { dismiss ->
                val options = iconOptions.map { (value, display) ->
                    ListOption(
                        titleText = display,
                        selected = value == selectedIconType
                    )
                }
                
                ListDialog(
                    state = rememberUseCaseState(visible = true, onCloseRequest = { dismiss() }),
                    header = Header.Default(title = "Select Home Screen Icon Style"),
                    selection = ListSelection.Single(
                        showRadioButtons = true,
                        options = options
                    ) { index, _ ->
                        val selectedIcon = iconOptions[index].first
                        prefs.edit().putString("selected_icon_type", selectedIcon).apply()
                        selectedIconType = selectedIcon
                        dismiss()
                    }
                )
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.Palette, "Home Screen Icon Style") },
                headlineContent = { Text(
                    text = "Home Screen Icon Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { Text("Change the home screen icon style or turn it off. Current: $currentIconDisplay") },
                modifier = Modifier
                    .clickable {
                        iconDialog.show()
                    }
            )

            // Icon Theme Selection
            var showIconThemeManager by remember { mutableStateOf(false) }
            var availableIconPacks by remember { mutableStateOf<List<IconPack>>(emptyList()) }
            
            // Load available icon packs to check if any are installed
            LaunchedEffect(Unit) {
                availableIconPacks = IconPackHelper.getInstalledIconPacks(context)
            }
            
            ListItem(
                leadingContent = { Icon(Icons.Filled.Style, stringResource(R.string.icon_theme)) },
                headlineContent = { Text(
                    text = stringResource(R.string.icon_theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { 
                    Text(
                        if (availableIconPacks.isEmpty()) {
                            "No themes installed. Install a theme"
                        } else {
                            stringResource(R.string.icon_theme_summary)
                        }
                    )
                },
                modifier = Modifier
                    .clickable {
                        showIconThemeManager = true
                    }
            )
            
            if (showIconThemeManager) {
                IconThemeManagerDialog(
                    onDismiss = { showIconThemeManager = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
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
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
