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
                    text = "Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                CardItemSpacer()
                
                CardRowContent(
                    icon = Icons.Filled.Palette,
                    text = "Theme Mode",
                    subtitle = "Current: $currentThemeDisplay",
                    modifier = Modifier.clickable {
                        themeDialog.show()
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
                        text = "Background",
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
                            title = "Background Blur",
                            subtitle = "Adjust the blur effect on the background image",
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
                        text = "Interface",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    CardItemSpacer()
                    
                    CardSliderContent(
                        title = "UI Transparency",
                        subtitle = "Adjust the transparency of UI elements",
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Display",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

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

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.dpi_scale_settings),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.dpi_scale_settings_summary, savedDpi),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${tempDpi}dpi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Slider(
                                 value = tempDpi.toFloat(),
                                 onValueChange = { newValue ->
                                     tempDpi = newValue.toInt()
                                 },
                                 valueRange = 100f..800f,
                                 modifier = Modifier.fillMaxWidth()
                             )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
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
                                    title = { Text("Set Custom DPI") },
                                    text = {
                                        Column {
                                            Text(
                                                text = "Enter a DPI value between 100 and 800:",
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
                                                label = { Text("DPI Value") },
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
                                            Text("Set")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showCustomDpiDialog = false
                                                customDpiText = ""
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                            

                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Reset button with Clear icon
                                IconButton(
                                    onClick = {
                                        tempDpi = systemDpi
                                        savedDpi = systemDpi
                                        prefs.edit().putInt("app_dpi", systemDpi).commit()
                                    },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Reset DPI"
                                    )
                                }
                                
                                // Custom/Preset button with Tune icon
                                Box {
                                    IconButton(
                                        onClick = { showDpiDropdown = true },
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Tune,
                                            contentDescription = "DPI Presets"
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
                                            text = { Text("Custom...") },
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
                                    enabled = tempDpi != savedDpi,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Confirm DPI"
                                    )
                                }
                            }
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
                text = "Theme Mode",
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
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    )
}
