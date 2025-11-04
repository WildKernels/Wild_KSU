package com.rifsxd.ksunext.ui.component.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.profile.Capabilities
import com.rifsxd.ksunext.profile.Groups
import com.rifsxd.ksunext.ui.util.isSepolicyValid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootProfileConfig(
    modifier: Modifier = Modifier,
    fixedName: Boolean,
    profile: Natives.Profile,
    onProfileChange: (Natives.Profile) -> Unit,
) {
    Column(modifier = modifier) {
        if (!fixedName) {
            OutlinedTextField(
                label = { Text(stringResource(R.string.profile_name)) },
                value = profile.name,
                onValueChange = { onProfileChange(profile.copy(name = it)) }
            )
        }

        /* 
        var expanded by remember { mutableStateOf(false) }
        val currentNamespace = when (profile.namespace) {
            Natives.Profile.Namespace.INHERITED.ordinal -> stringResource(R.string.profile_namespace_inherited)
            Natives.Profile.Namespace.GLOBAL.ordinal -> stringResource(R.string.profile_namespace_global)
            Natives.Profile.Namespace.INDIVIDUAL.ordinal -> stringResource(R.string.profile_namespace_individual)
            else -> stringResource(R.string.profile_namespace_inherited)
        }
        ListItem(headlineContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    readOnly = true,
                    label = { Text(stringResource(R.string.profile_namespace)) },
                    value = currentNamespace,
                    onValueChange = {},
                    trailingIcon = {
                        if (expanded) Icon(Icons.Filled.ArrowDropUp, null)
                        else Icon(Icons.Filled.ArrowDropDown, null)
                    },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_namespace_inherited)) },
                        onClick = {
                            onProfileChange(profile.copy(namespace = Natives.Profile.Namespace.INHERITED.ordinal))
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_namespace_global)) },
                        onClick = {
                            onProfileChange(profile.copy(namespace = Natives.Profile.Namespace.GLOBAL.ordinal))
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_namespace_individual)) },
                        onClick = {
                            onProfileChange(profile.copy(namespace = Natives.Profile.Namespace.INDIVIDUAL.ordinal))
                            expanded = false
                        },
                    )
                }
            }
        })
        */

        UidPanel(uid = profile.uid, label = "uid", onUidChange = {
            onProfileChange(
                profile.copy(
                    uid = it,
                    rootUseDefault = false
                )
            )
        })

        UidPanel(uid = profile.gid, label = "gid", onUidChange = {
            onProfileChange(
                profile.copy(
                    gid = it,
                    rootUseDefault = false
                )
            )
        })

        val selectedGroups = profile.groups.ifEmpty { listOf(0) }.let { e ->
            e.mapNotNull { g ->
                Groups.entries.find { it.gid == g }
            }
        }
        GroupsPanel(selectedGroups) {
            onProfileChange(
                profile.copy(
                    groups = it.map { group -> group.gid }.ifEmpty { listOf(0) },
                    rootUseDefault = false
                )
            )
        }

        val selectedCaps = profile.capabilities.mapNotNull { e ->
            Capabilities.entries.find { it.cap == e }
        }

        CapsPanel(selectedCaps) {
            onProfileChange(
                profile.copy(
                    capabilities = it.map { cap -> cap.cap },
                    rootUseDefault = false
                )
            )
        }

        SELinuxPanel(profile = profile, onSELinuxChange = { domain, rules ->
            onProfileChange(
                profile.copy(
                    context = domain,
                    rules = rules,
                    rootUseDefault = false
                )
            )
        })

    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupsPanel(selected: List<Groups>, closeSelection: (selection: Set<Groups>) -> Unit) {
    var showGroupsDialog by remember { mutableStateOf(false) }
    
    if (showGroupsDialog) {
        val groups = Groups.entries.toTypedArray().sortedWith(
            compareBy<Groups> { if (selected.contains(it)) 0 else 1 }
                .then(compareBy {
                    when (it) {
                        Groups.ROOT -> 0
                        Groups.SYSTEM -> 1
                        Groups.SHELL -> 2
                        else -> Int.MAX_VALUE
                    }
                })
                .then(compareBy { it.name })
        )
        
        var selection by remember { mutableStateOf(selected.toSet()) }
        
        AlertDialog(
            onDismissRequest = { showGroupsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f),
            title = {
                Text(
                    text = stringResource(R.string.profile_groups),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(groups) { group ->
                        val isSelected = selection.contains(group)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selection = if (isSelected) {
                                        selection - group
                                    } else {
                                        if (selection.size < 32) { // Kernel only supports 32 groups at most
                                            selection + group
                                        } else {
                                            selection
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.0f)
                                else 
                                    MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = group.display,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = group.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeSelection(selection)
                        showGroupsDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupsDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showGroupsDialog = true
                }
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.profile_groups))
            FlowRow {
                selected.forEach { group ->
                    AssistChip(
                        modifier = Modifier.padding(3.dp),
                        onClick = { /*TODO*/ },
                        label = { Text(group.display) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CapsPanel(
    selected: Collection<Capabilities>,
    closeSelection: (selection: Set<Capabilities>) -> Unit
) {
    var showCapabilitiesDialog by remember { mutableStateOf(false) }
    
    if (showCapabilitiesDialog) {
        val caps = Capabilities.entries.toTypedArray().sortedWith(
            compareBy<Capabilities> { if (selected.contains(it)) 0 else 1 }
                .then(compareBy { it.name })
        )
        val selection = remember { mutableStateOf(HashSet(selected)) }
        
        AlertDialog(
            onDismissRequest = { showCapabilitiesDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f),
            title = { Text(stringResource(R.string.profile_capabilities)) },
            text = {
                LazyColumn {
                    items(caps) { capability ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    val newSelection = HashSet(selection.value)
                                    if (newSelection.contains(capability)) {
                                        newSelection.remove(capability)
                                    } else {
                                        newSelection.add(capability)
                                    }
                                    selection.value = newSelection
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selection.value.contains(capability)) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.0f)
                                else 
                                    MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
                            ),
                            border = if (selection.value.contains(capability)) 
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selection.value.contains(capability),
                                    onCheckedChange = { checked ->
                                        val newSelection = HashSet(selection.value)
                                        if (checked) {
                                            newSelection.add(capability)
                                        } else {
                                            newSelection.remove(capability)
                                        }
                                        selection.value = newSelection
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = capability.display,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = capability.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeSelection(selection.value)
                        showCapabilitiesDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCapabilitiesDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showCapabilitiesDialog = true
                }
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.profile_capabilities))
            FlowRow {
                selected.forEach { group ->
                    AssistChip(
                        modifier = Modifier.padding(3.dp),
                        onClick = { /*TODO*/ },
                        label = { Text(group.display) })
                }
            }
        }
    }
}

@Composable
private fun UidPanel(uid: Int, label: String, onUidChange: (Int) -> Unit) {

    ListItem(headlineContent = {
        var isError by remember {
            mutableStateOf(false)
        }
        var lastValidUid by remember {
            mutableIntStateOf(uid)
        }
        val keyboardController = LocalSoftwareKeyboardController.current

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            value = uid.toString(),
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            onValueChange = {
                if (it.isEmpty()) {
                    onUidChange(0)
                    return@OutlinedTextField
                }
                val valid = isTextValidUid(it)

                val targetUid = if (valid) it.toInt() else lastValidUid
                if (valid) {
                    lastValidUid = it.toInt()
                }

                onUidChange(targetUid)

                isError = !valid
            }
        )
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SELinuxPanel(
    profile: Natives.Profile,
    onSELinuxChange: (domain: String, rules: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var domain by remember { mutableStateOf(profile.context) }
    var rules by remember { mutableStateOf(profile.rules) }

    val domainValid = remember(domain) {
        val regex = Regex("^[a-z_]+:[a-z0-9_]+:[a-z0-9_]+(:[a-z0-9_]+)?$")
        domain.matches(regex)
    }
    val rulesValid = remember(rules) { isSepolicyValid(rules) }
    val canConfirm = domainValid && rulesValid

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1.0f),
            title = { Text(text = stringResource(R.string.profile_selinux_context)) },
            text = {
                Column {
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.profile_selinux_domain)) },
                        value = domain,
                        onValueChange = { domain = it },
                        singleLine = true,
                        isError = !domainValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                    )
                    if (!domainValid) {
                        Text(
                            text = "Domain must be in the format of \"user:role:type:level\"",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.profile_selinux_rules)) },
                        value = rules,
                        onValueChange = { rules = it },
                        isError = !rulesValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                        ),
                        maxLines = 6
                    )
                    if (!rulesValid) {
                        Text(
                            text = "SELinux rules is invalid!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSELinuxChange(domain, rules)
                        showDialog = false
                    },
                    enabled = canConfirm
                ) { Text(text = stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    ListItem(headlineContent = {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            label = { Text(text = stringResource(R.string.profile_selinux_context)) },
            value = profile.context,
            onValueChange = { }
        )
    })
}

@Preview
@Composable
private fun RootProfileConfigPreview() {
    var profile by remember { mutableStateOf(Natives.Profile("")) }
    RootProfileConfig(fixedName = true, profile = profile) {
        profile = it
    }
}

private fun isTextValidUid(text: String): Boolean {
    return text.isNotEmpty() && text.isDigitsOnly() && text.toInt() >= 0
}
