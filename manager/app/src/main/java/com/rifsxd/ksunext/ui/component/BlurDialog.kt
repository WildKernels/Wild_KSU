package com.rifsxd.ksunext.ui.component

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rifsxd.ksunext.ui.util.LocalBaseColorScheme
import com.rifsxd.ksunext.ui.util.LocalUiOverlaySettings

@Composable
fun BlurDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseScheme = LocalBaseColorScheme.current
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        // Apply blur behind the popup window if transparency is active and supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val view = LocalView.current
            val context = LocalContext.current
            DisposableEffect(view) {
                val root = view.rootView
                val params = root.layoutParams as? WindowManager.LayoutParams
                if (params != null) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    params.blurBehindRadius = 60 // Blur radius in pixels
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm.updateViewLayout(root, params)
                }
                onDispose {}
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            colors = CardDefaults.cardColors(
                containerColor = baseScheme.surfaceContainer.copy(alpha = cardAlpha),
            ),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                content()
            }
        }
    }
}

@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentSelection by remember(selectedOption) { mutableStateOf(selectedOption) }

    BlurDialog(onDismissRequest = onDismissRequest) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { (option, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentSelection = option }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (currentSelection == option),
                        onClick = { currentSelection = option }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onOptionSelected(currentSelection)
                onDismissRequest()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}
