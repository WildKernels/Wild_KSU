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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Background Image Section
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
                            
                            ListItem(
                                leadingContent = { Icon(Icons.Filled.Opacity, stringResource(R.string.background_transparency)) },
                                headlineContent = { 
                                    Text(
                                        text = stringResource(R.string.background_transparency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    ) 
                                },
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
                                    }
                                }
                            )
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
                        
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.Tune, "UI Transparency") },
                            headlineContent = { 
                                Text(
                                    text = "UI Transparency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
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
                                }
                            }
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
                        var showDpiDialog by remember { mutableStateOf(false) }

                        ListItem(
                            leadingContent = { Icon(Icons.Filled.AspectRatio, stringResource(R.string.dpi_scale_settings)) },
                            headlineContent = { 
                                Text(
                                    text = stringResource(R.string.dpi_scale_settings),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                ) 
                            },
                            supportingContent = { 
                                Text(
                                    text = stringResource(R.string.dpi_scale_settings_summary, savedDpi),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { showDpiDialog = true }
                                ) {
                                    Text(
                                        text = stringResource(R.string.dpi_change_settings),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        )

                        // DPI Selection Dialog
                        if (showDpiDialog) {
                            AlertDialog(
                                onDismissRequest = { showDpiDialog = false },
                                title = { Text(stringResource(R.string.dpi_scale_settings)) },
                                text = {
                                    Column {
                                        Text(stringResource(R.string.dpi_dialog_description))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        val dpiOptions = listOf(
                                            120 to "LDPI (120)",
                                            160 to "MDPI (160)",
                                            240 to "HDPI (240)",
                                            320 to "XHDPI (320)",
                                            480 to "XXHDPI (480)",
                                            640 to "XXXHDPI (640)",
                                            systemDpi to "System Default ($systemDpi)"
                                        )
                                        
                                        dpiOptions.forEach { (dpi, label) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        savedDpi = dpi
                                                        prefs.edit().putInt("app_dpi", dpi).commit()
                                                        showDpiDialog = false
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = savedDpi == dpi,
                                                    onClick = {
                                                        savedDpi = dpi
                                                        prefs.edit().putInt("app_dpi", dpi).commit()
                                                        showDpiDialog = false
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(label)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showDpiDialog = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }

                        // Apply DPI Settings Button
                        if (savedDpi != systemDpi || prefs.contains("app_dpi")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        // Restart activity to apply DPI changes
                                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        if (context is android.app.Activity) {
                                            context.finish()
                                        }
                                    },
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
                }
            }
        }
    }
}