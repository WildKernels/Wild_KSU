package com.rifsxd.ksunext.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

@Immutable
data class BackgroundSettings(
    val uri: String?,
    val fillScreen: Boolean,
    val isVideo: Boolean,
)

val LocalBackgroundSettings = compositionLocalOf {
    BackgroundSettings(uri = null, fillScreen = false, isVideo = false)
}

@Immutable
data class UiOverlaySettings(
    val cardAlpha: Float,
    val dimAlpha: Float,
)

val LocalUiOverlaySettings = compositionLocalOf {
    UiOverlaySettings(cardAlpha = 1f, dimAlpha = 0f)
}
