package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.screen.IconThemeManagerDialog
import com.rifsxd.ksunext.ui.util.IconPackHelper
import com.rifsxd.ksunext.ui.util.IconPack

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuperuserSettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

                        // Individual App Cards Setting
                        var useIndividualCards by rememberSaveable {
                            mutableStateOf(
                                prefs.getBoolean("use_individual_app_cards", false)
                            )
                        }

                        SwitchItem(
                            icon = Icons.Filled.Apps,
                            title = stringResource(R.string.individual_app_cards),
                            summary = stringResource(R.string.individual_app_cards_summary),
                            checked = useIndividualCards
                        ) {
                            prefs.edit().putBoolean("use_individual_app_cards", it).apply()
                            useIndividualCards = it
                        }

                        // Enable Favorite Button
                        var enableFavoriteButton by rememberSaveable {
                            mutableStateOf(
                                !prefs.getBoolean("disable_favorite_button", true)
                            )
                        }
                        SwitchItem(
                            icon = Icons.Filled.Favorite,
                            title = "Enable Favorite Button",
                            summary = "Show favorite button on app cards to mark frequently used apps",
                            checked = enableFavoriteButton
                        ) {
                            prefs.edit().putBoolean("disable_favorite_button", !it).apply()
                            prefs.edit().putBoolean("enable_favorite_button", it).apply()
                            prefs.edit().putBoolean("disable_favorite_sorting", !it).apply()
                            enableFavoriteButton = it
                        }
                    }
                }
            }
        }
    }
}