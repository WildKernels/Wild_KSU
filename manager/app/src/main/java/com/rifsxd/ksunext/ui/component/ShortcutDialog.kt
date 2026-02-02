package com.rifsxd.ksunext.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.util.LocalBaseColorScheme
import com.rifsxd.ksunext.ui.util.LocalUiOverlaySettings
import com.rifsxd.ksunext.ui.util.module.Shortcut

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun ShortcutDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var iconUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val baseScheme = LocalBaseColorScheme.current
    val cardAlpha = LocalUiOverlaySettings.current.cardAlpha

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            iconUri = it.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        // Apply blur behind the popup window if transparency is active and supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val view = LocalView.current
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
                Text(
                    text = stringResource(R.string.module_shortcut_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.module_shortcut_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launcher.launch("image/*") }
                ) {
                    val bitmap = remember(iconUri) {
                        Shortcut.loadShortcutBitmap(context, iconUri)
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder or default icon
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.mipmap.ic_launcher)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = stringResource(R.string.module_shortcut_icon_pick),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, iconUri) },
                        enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.module_shortcut_add))
                    }
                }
            }
        }
    }
}
