package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.ui.component.SwitchItem

@Destination<RootGraph>
@Composable
fun ModuleSettingsScreen(
    navigator: DestinationsNavigator
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        ModuleSettingsContent(
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun ModuleSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Module Cards Always Expanded Setting
        var keepModulesExpanded by rememberSaveable {
            mutableStateOf(
                prefs.getBoolean("keep_modules_expanded", false)
            )
        }
        SwitchItem(
            icon = Icons.Filled.ExpandMore,
            title = "Keep Module Cards Expanded",
            summary = "Always keep module cards expanded instead of collapsing them",
            checked = keepModulesExpanded
        ) {
            prefs.edit().putBoolean("keep_modules_expanded", it).apply()
            keepModulesExpanded = it
        }

        // Hide Module Details Text Setting
        var hideModuleDetails by rememberSaveable {
            mutableStateOf(
                prefs.getBoolean("hide_module_details", false)
            )
        }
        SwitchItem(
            icon = Icons.Filled.VisibilityOff,
            title = "Hide Module Details",
            summary = "Hide descriptive text like module size, web UI, action, and Zygisk requirements",
            checked = hideModuleDetails
        ) {
            prefs.edit().putBoolean("hide_module_details", it).apply()
            hideModuleDetails = it
        }
    }
}



@Preview
@Composable
private fun ModuleSettingsPreview() {
    ModuleSettingsScreen(EmptyDestinationsNavigator)
}