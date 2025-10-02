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
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.CardConstants
import com.rifsxd.ksunext.ui.component.CardRowContent
import com.rifsxd.ksunext.ui.component.CardSwitchContent
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import com.rifsxd.ksunext.ui.component.rememberNoRippleInteractionSource
import com.rifsxd.ksunext.ui.screen.IconThemeManagerDialog
import com.rifsxd.ksunext.ui.util.IconPackHelper
import com.rifsxd.ksunext.ui.util.IconPack

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuperuserSettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CardConstants.CARD_PADDING_MEDIUM),
        verticalArrangement = Arrangement.spacedBy(CardConstants.CARD_PADDING_MEDIUM)
    ) {
        // App Display Settings Card
        item {
            StandardCard {
                // Icon Theme Selection
                var showIconThemeManager by remember { mutableStateOf(false) }
                var availableIconPacks by remember { mutableStateOf<List<IconPack>>(emptyList()) }
                
                // Load available icon packs to check if any are installed
                LaunchedEffect(Unit) {
                    availableIconPacks = IconPackHelper.getInstalledIconPacks(context)
                }
                
                CardRowContent(
                    text = stringResource(R.string.icon_theme),
                    icon = Icons.Filled.Style,
                    modifier = Modifier.clickable(
                        onClick = { showIconThemeManager = true },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )
                
                if (showIconThemeManager) {
                    IconThemeManagerDialog(
                        onDismiss = { showIconThemeManager = false }
                    )
                }

                CardItemSpacer()

                // Individual App Cards Setting
                var useIndividualCards by remember {
                    mutableStateOf(
                        prefs.getBoolean("use_individual_app_cards", false)
                    )
                }

                // Listen for preference changes
                DisposableEffect(Unit) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == "use_individual_app_cards") {
                            useIndividualCards = prefs.getBoolean("use_individual_app_cards", false)
                        }
                    }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                CardSwitchContent(
                    title = stringResource(R.string.individual_app_cards),
                    icon = Icons.Filled.Apps,
                    checked = useIndividualCards,
                    onCheckedChange = { checked ->
                        useIndividualCards = checked
                        prefs.edit().putBoolean("use_individual_app_cards", checked).apply()
                    }
                )

                CardItemSpacer()

                // Enable Favorite Button
                var enableFavoriteButton by rememberSaveable {
                    mutableStateOf(
                        !prefs.getBoolean("disable_favorite_button", true)
                    )
                }
                
                CardSwitchContent(
                    title = "Enable Favorite Button",
                    icon = Icons.Filled.Favorite,
                    checked = enableFavoriteButton,
                    onCheckedChange = { checked ->
                        enableFavoriteButton = checked
                        prefs.edit().putBoolean("disable_favorite_button", !checked).apply()
                        prefs.edit().putBoolean("enable_favorite_button", checked).apply()
                        prefs.edit().putBoolean("disable_favorite_sorting", !checked).apply()
                    }
                )
            }
        }
    }
}
