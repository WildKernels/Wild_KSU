package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.CardSwitchContent
import com.rifsxd.ksunext.ui.component.CardItemSpacer

/**
 * @author rifsxd
 * @date 2025/6/15.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DeveloperScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    val isManager = Natives.becomeManager(context.packageName)
    val ksuVersion = if (isManager) Natives.version else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StandardCard {
                var developerOptionsEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_developer_options", false)
                    )
                }
                
                var enableWebDebugging by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_web_debugging", false)
                    )
                }
                
                if (ksuVersion != null) {
                    CardSwitchContent(
                        icon = Icons.Filled.DeveloperMode,
                        title = stringResource(R.string.enable_developer_options),
                        subtitle = stringResource(R.string.enable_developer_options_summary),
                        checked = developerOptionsEnabled
                    ) {
                        prefs.edit().putBoolean("enable_developer_options", it).apply()
                        developerOptionsEnabled = it
                    }
                    
                    CardItemSpacer()
                    
                    CardSwitchContent(
                        enabled = developerOptionsEnabled,
                        icon = Icons.Filled.Web,
                        title = stringResource(R.string.enable_web_debugging),
                        subtitle = stringResource(R.string.enable_web_debugging_summary),
                        checked = enableWebDebugging
                    ) {
                        prefs.edit().putBoolean("enable_web_debugging", it).apply()
                        enableWebDebugging = it
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeveloperPreview() {
    DeveloperScreen(EmptyDestinationsNavigator)
}
