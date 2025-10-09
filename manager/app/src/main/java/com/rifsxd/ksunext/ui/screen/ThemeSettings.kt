package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
// Removed Save icon import - no longer needed
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.CardRowContent
import com.rifsxd.ksunext.ui.component.CardSwitchContent
import com.rifsxd.ksunext.ui.component.CardSliderContent
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.PhotoEditorScreenDestination
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.util.BackgroundCustomization
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ThemeSettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Theme mode state
    var themeMode by rememberSaveable {
        mutableStateOf(prefs.getString("theme_mode", "system_default") ?: "system_default")
    }

    // Background image state
    var backgroundImageUri by rememberSaveable {
        mutableStateOf(prefs.getString("background_image_uri", null))
    }
    
    // Bottom bar toggle state
    var hideBottomBar by rememberSaveable {
        mutableStateOf(prefs.getBoolean("hide_bottom_bar", false))
    }
    
    // Sync state with actual saved preferences when returning from other screens
    LaunchedEffect(Unit) {
        // Update backgroundImageUri from SharedPreferences
        val currentSavedUri = prefs.getString("background_image_uri", null)
        if (backgroundImageUri != currentSavedUri) {
            backgroundImageUri = currentSavedUri
        }
    }

    // Image picker launcher
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    // Copy image to internal storage for editing
                    val savedPath = BackgroundCustomization.copyImageToInternalStorage(context, uri)
                    if (savedPath != null) {
                        val savedUri = BackgroundCustomization.filePathToUri(savedPath)
                        // Navigate directly to photo editor - saving will happen there
                        navigator.navigate(PhotoEditorScreenDestination(imageUri = savedUri.toString()))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme Mode Section
        item {
            val themeOptions = listOf(
                "system_default" to "System Default",
                "light" to "Light Mode",
                "dark" to "Dark Mode",
                "amoled" to "AMOLED Dark"
            )
            
            val currentThemeDisplay = themeOptions.find { it.first == themeMode }?.second ?: "System Default"
            
            val themeDialog = rememberCustomDialog { dismiss: () -> Unit ->
                ThemeSelectionDialog(
                    themeOptions = themeOptions,
                    currentTheme = themeMode,
                    onThemeSelected = { selectedTheme ->
                        prefs.edit().putString("theme_mode", selectedTheme).commit()
                        themeMode = selectedTheme
                        dismiss()
                    },
                    onDismiss = { dismiss() }
                )
            }
            
            StandardCard {
                    Text(
                        text = stringResource(R.string.theme_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    CardItemSpacer()
                    
                    CardRowContent(
                        icon = Icons.Filled.Palette,
                        text = stringResource(R.string.theme_mode),
                        subtitle = stringResource(R.string.theme_mode_summary, currentThemeDisplay),
                        modifier = Modifier.clickable {
                            themeDialog.show()
                        }
                    )
                }
        }
        
        // Navigation Section
        item {
            StandardCard {
                Text(
                    text = stringResource(R.string.navigation_section_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                CardItemSpacer()
                
                CardSwitchContent(
                    title = stringResource(R.string.hide_bottom_bar),
                    subtitle = stringResource(R.string.hide_bottom_bar_summary),
                    icon = Icons.Filled.ViewAgenda,
                    checked = hideBottomBar,
                    onCheckedChange = { enabled ->
                        hideBottomBar = enabled
                        prefs.edit().putBoolean("hide_bottom_bar", enabled).commit()
                    }
                )
            }
        }

            // Background Image Section
            item {
                // Background Transparency and Blur sliders state
                var backgroundTransparency by rememberSaveable {
                    mutableFloatStateOf(
                        prefs.getFloat("background_transparency", 0.0f)
                    )
                }
                
                var backgroundBlur by rememberSaveable {
                    mutableFloatStateOf(
                        prefs.getFloat("background_blur", 0.0f)
                    )
                }
                
                StandardCard {
                    Text(
                        text = stringResource(R.string.background_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    CardItemSpacer()
                    
                    // Background Image Selection
                    CardRowContent(
                        text = stringResource(R.string.settings_background_image),
                        subtitle = stringResource(R.string.settings_background_image_summary),
                        icon = Icons.Filled.Image,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "image/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            selectImageLauncher.launch(intent)
                        },
                        actions = {
                            Row {
                                // Use the saved background image for buttons
                                val activeImageUri = backgroundImageUri
                                
                                // Crop button (only show if background image is selected)
                                if (activeImageUri != null) {
                                    IconButton(onClick = {
                                        // Navigate to PhotoEditor for current image
                                        navigator.navigate(PhotoEditorScreenDestination(imageUri = activeImageUri))
                                    }) {
                                        Icon(
                                            Icons.Filled.Crop, 
                                            stringResource(R.string.crop_background_image),
                                            tint = Color.White
                                        )
                                    }
                                }
                                // Delete button (only show if background image is selected)
                                if (activeImageUri != null) {
                                    IconButton(onClick = {
                                        // Clean up internal storage if the image was stored there
                                        BackgroundCustomization.deleteInternalBackgroundImage(context)
                                        prefs.edit().remove("background_image_uri").commit()
                                        backgroundImageUri = null
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete, 
                                            stringResource(R.string.background_image_remove),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    )
                    
                    // Background Transparency Slider (only show when background image is enabled)
                    if (backgroundImageUri != null) {
                        CardItemSpacer()
                        
                        CardSliderContent(
                            title = stringResource(R.string.background_transparency),
                            subtitle = stringResource(R.string.background_transparency_summary),
                            icon = Icons.Filled.Opacity,
                            value = backgroundTransparency,
                            valueRange = 0.0f..1.0f,
                            valueDisplay = "${(backgroundTransparency * 100).toInt()}%",
                            iconTint = Color.White,
                            onValueChange = { value ->
                                backgroundTransparency = value
                                prefs.edit().putFloat("background_transparency", value).apply()
                            }
                        )
                        
                        CardItemSpacer()
                        
                        // Background Blur Slider
                        CardSliderContent(
                            title = stringResource(R.string.background_blur),
                            subtitle = stringResource(R.string.background_blur_summary),
                            icon = Icons.Filled.Tune,
                            value = backgroundBlur,
                            valueRange = 0.0f..50.0f,
                            valueDisplay = "${backgroundBlur.toInt()}px",
                            iconTint = Color.White,
                            onValueChange = { value ->
                                backgroundBlur = value
                                prefs.edit().putFloat("background_blur", value).apply()
                            }
                        )
                    }
                }
            }

            // UI Transparency Section
            item {
                // UI Transparency Slider
                var uiTransparency by rememberSaveable {
                    mutableFloatStateOf(
                        prefs.getFloat("ui_transparency", 0.0f)
                    )
                }
                
                StandardCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.interface_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    CardItemSpacer()
                    
                    CardSliderContent(
                        title = stringResource(R.string.ui_transparency),
                        subtitle = stringResource(R.string.ui_transparency_summary),
                        icon = Icons.Filled.Tune,
                        value = uiTransparency,
                        valueRange = 0.0f..1.0f,
                        valueDisplay = "${(uiTransparency * 100).toInt()}%",
                        iconTint = Color.White,
                        onValueChange = { value ->
                            uiTransparency = value
                            prefs.edit().putFloat("ui_transparency", value).commit()
                        }
                    )
                }
            }

            // DPI Scale Section
            item {
                // DPI Scale Settings
                val systemDpi = remember { 
                    // Store original system DPI on first run
                    if (!prefs.contains("original_system_dpi")) {
                        val originalDpi = context.resources.displayMetrics.densityDpi
                        prefs.edit().putInt("original_system_dpi", originalDpi).commit()
                        originalDpi
                    } else {
                        prefs.getInt("original_system_dpi", 160) // 160 is default Android DPI
                    }
                }
                var savedDpi by remember { 
                    mutableIntStateOf(
                        if (prefs.contains("app_dpi")) {
                            prefs.getInt("app_dpi", systemDpi)
                        } else {
                            systemDpi
                        }
                    )
                }
                var tempDpi by remember { mutableIntStateOf(savedDpi) }
                
                StandardCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.display_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    CardItemSpacer()

                    CardSliderContent(
                        title = stringResource(R.string.dpi_scale_settings),
                        subtitle = stringResource(R.string.dpi_scale_settings_summary, savedDpi),
                        icon = Icons.Filled.AspectRatio,
                        value = tempDpi.toFloat(),
                        valueRange = 100f..800f,
                        valueDisplay = "${tempDpi}dpi",
                        iconTint = Color.White,
                        onValueChange = { value ->
                            tempDpi = value.toInt()
                        }
                    )
                    
                    CardItemSpacer()
                            
                    // DPI Preset Values
                    val dpiPresets = listOf(
                        120 to "LDPI (120)",
                        160 to "MDPI (160)",
                        240 to "HDPI (240)",
                        320 to "XHDPI (320)",
                        480 to "XXHDPI (480)",
                        640 to "XXXHDPI (640)"
                    )
                    
                    // Dropdown menu state
                    var showDpiDropdown by remember { mutableStateOf(false) }
                    
                    // Custom DPI Input Dialog
                    var showCustomDpiDialog by remember { mutableStateOf(false) }
                    var customDpiText by remember { mutableStateOf("") }
                            
                    if (showCustomDpiDialog) {
                        AlertDialog(
                            onDismissRequest = { showCustomDpiDialog = false },
                            title = { Text(stringResource(R.string.set_custom_dpi)) },
                            text = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.custom_dpi_prompt),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    OutlinedTextField(
                                        value = customDpiText,
                                        onValueChange = { newValue ->
                                            // Only allow numeric input
                                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                                customDpiText = newValue
                                            }
                                        },
                                        label = { Text(stringResource(R.string.dpi_value)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val dpiValue = customDpiText.toIntOrNull()
                                        if (dpiValue != null && dpiValue in 100..800) {
                                            tempDpi = dpiValue
                                            savedDpi = dpiValue
                                            prefs.edit().putInt("app_dpi", dpiValue).commit()
                                            showCustomDpiDialog = false
                                            customDpiText = ""
                                        }
                                    },
                                    enabled = {
                                        val dpiValue = customDpiText.toIntOrNull()
                                        dpiValue != null && dpiValue in 100..800
                                    }()
                                ) {
                                    Text(stringResource(R.string.set))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showCustomDpiDialog = false
                                        customDpiText = ""
                                    }
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                    
                    CardRowContent(
                        text = stringResource(R.string.dpi_actions),
                        subtitle = stringResource(R.string.dpi_actions_summary),
                        icon = Icons.Filled.Settings
                    ) {
                        // Reset button with Clear icon
                        IconButton(
                            onClick = {
                                tempDpi = systemDpi
                                savedDpi = systemDpi
                                prefs.edit().putInt("app_dpi", systemDpi).commit()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.reset_dpi),
                                tint = Color.White
                            )
                        }
                        
                        // Custom/Preset button with Tune icon
                        Box {
                            IconButton(
                                onClick = { showDpiDropdown = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = stringResource(R.string.dpi_presets),
                                    tint = Color.White
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showDpiDropdown,
                                onDismissRequest = { showDpiDropdown = false }
                            ) {
                                dpiPresets.forEach { (dpi, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            tempDpi = dpi
                                            savedDpi = dpi
                                            prefs.edit().putInt("app_dpi", dpi).commit()
                                            showDpiDropdown = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.custom_option)) },
                                    onClick = {
                                        customDpiText = tempDpi.toString()
                                        showCustomDpiDialog = true
                                        showDpiDropdown = false
                                    }
                                )
                            }
                        }
                        
                        // Confirm button with Check icon
                        IconButton(
                            onClick = {
                                savedDpi = tempDpi
                                prefs.edit().putInt("app_dpi", savedDpi).commit()
                            },
                            enabled = tempDpi != savedDpi
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.confirm_dpi),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    themeOptions: List<Pair<String, String>>,
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.theme_mode_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                themeOptions.forEach { (value, display) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(value)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == currentTheme,
                            onClick = {
                                onThemeSelected(value)
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = display,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    )
}
