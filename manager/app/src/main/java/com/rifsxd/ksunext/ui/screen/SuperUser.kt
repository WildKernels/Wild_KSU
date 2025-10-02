package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.SearchAppBar
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel
import kotlinx.coroutines.launch
import com.rifsxd.ksunext.ui.LocalSuperUserViewModel
import com.rifsxd.ksunext.ui.component.ThemedIcon
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.CardType

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuperUserScreen(navigator: DestinationsNavigator) {
    val viewModel = LocalSuperUserViewModel.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // Make useIndividualCards reactive to preference changes
    var useIndividualCards by remember { mutableStateOf(prefs.getBoolean("use_individual_app_cards", false)) }
    
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

    LaunchedEffect(navigator) {
        viewModel.search = ""
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    PullToRefreshBox(
        onRefresh = {
            scope.launch { viewModel.fetchAppList() }
        },
        isRefreshing = viewModel.isRefreshing
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (useIndividualCards) PaddingValues(16.dp) else PaddingValues(0.dp),
            verticalArrangement = if (useIndividualCards) Arrangement.spacedBy(16.dp) else Arrangement.Top
        ) {
            items(viewModel.appList, key = { it.packageName + it.uid }) { app ->
                AppItem(app, prefs, useIndividualCards) {
                    navigator.navigate(AppProfileScreenDestination(app))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: SuperUserViewModel.AppInfo,
    prefs: android.content.SharedPreferences,
    useIndividualCards: Boolean,
    onClickListener: () -> Unit,
) {
    val viewModel = LocalSuperUserViewModel.current
    val disableFavoriteButton = prefs.getBoolean("disable_favorite_button", true)
    
    val content = @Composable {
        ListItem(
            modifier = Modifier.clickable(onClick = onClickListener),
            headlineContent = { Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            ) },
            supportingContent = {
                Column {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (app.allowSu) {
                            LabelItem(
                                text = "ROOT",
                            )
                        } else {
                            if (Natives.uidShouldUmount(app.uid)) {
                                LabelItem(
                                    text = "UMOUNT",
                                    style = LabelItemDefaults.style.copy(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        if (app.hasCustomProfile) {
                            LabelItem(
                                text = "CUSTOM",
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            )
                        } else if (!app.allowSu && !Natives.uidShouldUmount(app.uid)) {
                            LabelItem(
                                text = "DEFAULT",
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            )
                        }
                    }
                }
            },
            leadingContent = {
                ThemedIcon(
                    packageName = app.packageName,
                    packageInfo = app.packageInfo,
                    contentDescription = app.label,
                    modifier = Modifier
                        .padding(4.dp)
                        .width(48.dp)
                        .height(48.dp)
                )
            },
            trailingContent = if (!disableFavoriteButton) {
                {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(app.packageName) }
                    ) {
                        Icon(
                            imageVector = if (app.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (app.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (app.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            colors = if (useIndividualCards) {
                ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            } else {
                ListItemDefaults.colors()
            }
        )
    }
    
    if (useIndividualCards) {
        StandardCard(
            modifier = Modifier.fillMaxWidth(),
            cardType = CardType.SURFACE
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun LabelText(label: String) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp)
            .background(
                Color.Black,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 5.dp),
            style = TextStyle(
                fontSize = 8.sp,
                color = Color.White,
            )
        )
    }
}
