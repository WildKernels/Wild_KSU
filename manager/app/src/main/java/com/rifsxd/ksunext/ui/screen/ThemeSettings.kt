package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio

import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
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

    // Image picker launcher
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    // Copy image to internal storage and save URI
                    val savedPath = BackgroundCustomization.copyImageToInternalStorage(context, uri)
                    if (savedPath != null) {
                        val savedUri = BackgroundCustomization.filePathToUri(savedPath)
                        prefs.edit().putString("background_image_uri", savedUri).commit()
                        backgroundImageUri = savedUri.toString()
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val themeOptions = listOf(
                    "system_default" to "System Default",
                    "light" to "Light Mode",
                    "dark" to "Dark Mode",
                    "amoled" to "AMOLED Dark"
                )
                
                val currentThemeDisplay = themeOptions.find { it.first == themeMode }?.second ?: "System Default"
                
                val themeDialog = rememberCustomDialog { dismiss ->
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
            }
        }

            // Background Image Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Background",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                        // Background Image Selection
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.Image, stringResource(R.string.settings_background_image)) },
                            headlineContent = { 
                                Text(
                                    text = stringResource(R.string.settings_background_image),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                ) 
                            },
                            supportingContent = { Text(stringResource(R.string.settings_background_image_summary)) },
                            trailingContent = {
                                Row {
                                    // Crop button (only show if background image is selected)
                                    if (backgroundImageUri != null) {
                                        IconButton(onClick = {
                                            // Navigate to PhotoEditor for current image
                                            backgroundImageUri?.let { uriString ->
                                                navigator.navigate(PhotoEditorScreenDestination(imageUri = uriString))
                                            }
                                        }) {
                                            Icon(Icons.Filled.Crop, stringResource(R.string.crop_background_image))
                                        }
                                    }
                                    // Delete button (only show if background image is selected)
                                    if (backgroundImageUri != null) {
                                        IconButton(onClick = {
                                            // Clean up internal storage if the image was stored there
                                            BackgroundCustomization.deleteInternalBackgroundImage(context)
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

                        // Background Transparency Slider (only show when background image is enabled)
                        if (backgroundImageUri != null) {
                            var backgroundTransparency by rememberSaveable {
                                mutableFloatStateOf(
                                    prefs.getFloat("background_transparency", 0.0f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Opacity,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.background_transparency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.background_transparency_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(backgroundTransparency * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = backgroundTransparency,
                    onValueChange = { value ->
                        backgroundTransparency = value
                        prefs.edit().putFloat("background_transparency", value).commit()
                    },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
                            
                            // Background Blur Slider
                            var backgroundBlur by rememberSaveable {
                                mutableFloatStateOf(
                                    prefs.getFloat("background_blur", 0.0f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Background Blur",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Adjust the blur effect on the background image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${backgroundBlur.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = backgroundBlur,
                    onValueChange = { value ->
                        backgroundBlur = value
                        prefs.edit().putFloat("background_blur", value).commit()
                    },
                    valueRange = 0.0f..50.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
                        }
                    }
                }
            }

            // UI Transparency Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Interface",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // UI Transparency Slider
                        var uiTransparency by rememberSaveable {
                            mutableFloatStateOf(
                                prefs.getFloat("ui_transparency", 0.0f)
                            )
                        }
                        
                        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "UI Transparency",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Adjust the transparency of UI elements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(uiTransparency * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = uiTransparency,
                onValueChange = { value ->
                    uiTransparency = value
                    prefs.edit().putFloat("ui_transparency", value).commit()
                },
                valueRange = 0.0f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
                }
            }

            // DPI Scale Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        val systemDpi = remember { context.resources.displayMetrics.densityDpi }
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
                                .padding(horizontal = 4.dp)
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
                            Slider(
                                value = tempDpi.toFloat(),
                                onValueChange = { newValue ->
                                    val snapDpiValues = listOf(120, 220, 320, 420, 520, 620)
                                    val snapThreshold = 20
                                    
                                    val snappedValue = snapDpiValues.find { dpi ->
                                        kotlin.math.abs(newValue - dpi) <= snapThreshold
                                    } ?: newValue.toInt()
                                    
                                    tempDpi = snappedValue
                                },
                                valueRange = 120f..640f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        tempDpi = systemDpi
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Reset")
                                }
                                
                                Button(
                                    onClick = {
                                        savedDpi = tempDpi
                                        prefs.edit().putInt("app_dpi", savedDpi).commit()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = tempDpi != savedDpi
                                ) {
                                    Text("Confirm")
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
private fun ThemeSelectionDialog(
    themeOptions: List<Pair<String, String>>,
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = themeOptions.map { (value, display) ->
        ListOption(
            titleText = display,
            selected = value == currentTheme
        )
    }
    
    // Wrap the dialog in a Card to provide background
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        ListDialog(
            state = rememberUseCaseState(visible = true, onCloseRequest = { onDismiss() }),
            header = Header.Default(title = "Theme Mode"),
            selection = ListSelection.Single(
                showRadioButtons = true,
                options = options
            ) { index, _ ->
                val selectedTheme = themeOptions[index].first
                onThemeSelected(selectedTheme)
            }
        )
    }
}