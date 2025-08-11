package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rifsxd.ksunext.ui.component.IconThemeManagerDialog
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
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

            item {
                // Individual App Cards Setting
                var useIndividualCards by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("use_individual_app_cards", false)
                    )
                }

                SwitchItem(
                    icon = Icons.Filled.Apps,
                    title = "Individual App Cards",
                    summary = "Show each app in its own card instead of grouped view",
                    checked = useIndividualCards
                ) {
                    prefs.edit().putBoolean("use_individual_app_cards", it).apply()
                    useIndividualCards = it
                }
            }

            item {
                // Disable Favorite Button Setting (includes sorting)
                var disableFavoriteButton by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("disable_favorite_button", false)
                    )
                }

                SwitchItem(
                    icon = Icons.Filled.BookmarkBorder,
                    title = "Disable Favorite Button",
                    summary = "Hide the favorite button and disable favorite sorting",
                    checked = disableFavoriteButton
                ) {
                    prefs.edit().apply {
                        putBoolean("disable_favorite_button", it)
                        // Also set the sorting preference to match
                        putBoolean("disable_favorite_sorting", it)
                        apply()
                    }
                    disableFavoriteButton = it
                }
            }
        }
    }
}