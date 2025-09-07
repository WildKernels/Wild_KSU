package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.HomeSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperuserSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.CardConstants
import com.rifsxd.ksunext.ui.component.CardRowContent
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import com.rifsxd.ksunext.ui.component.rememberNoRippleInteractionSource
import com.rifsxd.ksunext.ui.util.LocaleHelper
import java.util.Locale

/**
 * @author rifsxd
 * @date 2025/6/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
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
                    // Get available locales using LocaleHelper utility
                    val supportedLocales = remember {
                        LocaleHelper.getAvailableLocales(context)
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
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CardConstants.CARD_PADDING_MEDIUM),
        verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_MEDIUM)
    ) {
        item {
            StandardCard {
                CardRowContent(
                    icon = Icons.Filled.Translate,
                    text = language,
                    subtitle = currentLanguageDisplay,
                    modifier = Modifier.clickable(
                        onClick = { languageDialog.show() },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                CardRowContent(
                    icon = Icons.Filled.Palette,
                    text = stringResource(R.string.theme_settings),
                    subtitle = stringResource(R.string.theme_settings_summary),
                    modifier = Modifier.clickable(
                        onClick = { navigator.navigate(ThemeSettingsScreenDestination) },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                CardRowContent(
                    icon = Icons.Filled.Info,
                    text = stringResource(R.string.info_card_customization),
                    subtitle = stringResource(R.string.info_card_customization_summary),
                    modifier = Modifier.clickable(
                        onClick = { navigator.navigate(HomeSettingsScreenDestination) },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                CardRowContent(
                    icon = Icons.Filled.SupervisorAccount,
                    text = stringResource(R.string.superuser_settings),
                    subtitle = stringResource(R.string.superuser_settings_summary),
                    modifier = Modifier.clickable(
                        onClick = { navigator.navigate(SuperuserSettingsScreenDestination) },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                CardRowContent(
                    icon = Icons.Filled.Extension,
                    text = stringResource(R.string.module_card_customization),
                    subtitle = stringResource(R.string.module_card_customization_summary),
                    modifier = Modifier.clickable(
                        onClick = { navigator.navigate(ModuleSettingsScreenDestination) },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
