package com.rifsxd.ksunext.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.profile.RootProfileConfig
import com.rifsxd.ksunext.ui.util.deleteAppProfileTemplate
import com.rifsxd.ksunext.ui.util.getAppProfileTemplate
import com.rifsxd.ksunext.ui.util.setAppProfileTemplate
import com.rifsxd.ksunext.ui.viewmodel.TemplateViewModel
import com.rifsxd.ksunext.ui.viewmodel.toJSON

/**
 * @author weishu
 * @date 2023/10/20.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun TemplateEditorScreen(
    navigator: ResultBackNavigator<Boolean>,
    initialTemplate: TemplateViewModel.TemplateInfo,
    readOnly: Boolean = true,
) {

    val isCreation = initialTemplate.id.isBlank()
    val autoSave = !isCreation

    var template by rememberSaveable {
        mutableStateOf(initialTemplate)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    BackHandler {
        navigator.navigateBack(result = !readOnly)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isCreation) stringResource(R.string.app_profile_template_create) else template.name,
                readOnly = readOnly,
                summary = if (readOnly) stringResource(R.string.app_profile_template_readonly) else "",
                onBack = dropUnlessResumed { navigator.navigateBack(result = !readOnly) },
                onDelete = {
                    if (!isCreation) {
                        deleteAppProfileTemplate(template.id)
                        navigator.navigateBack(result = true)
                    }
                },
                onSave = {
                    if (isCreation) {
                        if (saveTemplate(template, isCreation = true)) {
                            Toast.makeText(context, R.string.app_profile_template_import_success, Toast.LENGTH_SHORT).show()
                            navigator.navigateBack(result = true)
                        } else {
                            Toast.makeText(context, R.string.app_profile_template_save_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Template Basic Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Template Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (isCreation) {
                            var errorHint by remember {
                                mutableStateOf("")
                            }
                            val idConflictError = stringResource(id = R.string.app_profile_template_id_exist)
                            val idInvalidError = stringResource(id = R.string.app_profile_template_id_invalid)
                            TextEdit(
                                label = stringResource(id = R.string.app_profile_template_id),
                                text = template.id,
                                errorHint = errorHint,
                                isError = errorHint.isNotEmpty()
                            ) { value ->
                                errorHint = if (isTemplateExist(value)) {
                                    idConflictError
                                } else if (!isValidTemplateId(value)) {
                                    idInvalidError
                                } else {
                                    ""
                                }
                                template = template.copy(id = value)
                            }
                        }

                        TextEdit(
                            label = stringResource(id = R.string.app_profile_template_name),
                            text = template.name
                        ) { value ->
                            template.copy(name = value).run {
                                if (autoSave) {
                                    if (!saveTemplate(this)) {
                                        // failed
                                        return@run
                                    }
                                }
                                template = this
                            }
                        }
                        TextEdit(
                            label = stringResource(id = R.string.app_profile_template_description),
                            text = template.description
                        ) { value ->
                            template.copy(description = value).run {
                                if (autoSave) {
                                    if (!saveTemplate(this)) {
                                        // failed
                                        return@run
                                    }
                                }
                                template = this
                            }
                        }
                    }
                }
            }

            // Root Profile Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Root Profile Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        RootProfileConfig(fixedName = true,
                            profile = toNativeProfile(template),
                            onProfileChange = {
                                template.copy(
                                    uid = it.uid,
                                    gid = it.gid,
                                    groups = it.groups,
                                    capabilities = it.capabilities,
                                    context = it.context,
                                    namespace = it.namespace,
                                    rules = it.rules.split("\n")
                                ).run {
                                    if (autoSave) {
                                        if (!saveTemplate(this)) {
                                            // failed
                                            return@run
                                        }
                                    }
                                    template = this
                                }
                            })
                    }
                }
            }
        }
    }
}

fun toNativeProfile(templateInfo: TemplateViewModel.TemplateInfo): Natives.Profile {
    return Natives.Profile().copy(rootTemplate = templateInfo.id,
        uid = templateInfo.uid,
        gid = templateInfo.gid,
        groups = templateInfo.groups,
        capabilities = templateInfo.capabilities,
        context = templateInfo.context,
        namespace = templateInfo.namespace,
        rules = templateInfo.rules.joinToString("\n").ifBlank { "" })
}

fun isTemplateValid(template: TemplateViewModel.TemplateInfo): Boolean {
    if (template.id.isBlank()) {
        return false
    }

    if (!isValidTemplateId(template.id)) {
        return false
    }

    return true
}

fun saveTemplate(template: TemplateViewModel.TemplateInfo, isCreation: Boolean = false): Boolean {
    if (!isTemplateValid(template)) {
        return false
    }

    if (isCreation && isTemplateExist(template.id)) {
        return false
    }

    val json = template.toJSON()
    json.put("local", true)
    return setAppProfileTemplate(template.id, json.toString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: String,
    readOnly: Boolean,
    summary: String = "",
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
    onSave: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(surfaceContainer) { surfaceContainer }
    
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }, navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        }, actions = {
            if (readOnly) {
                return@TopAppBar
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = stringResource(id = R.string.app_profile_template_delete)
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(id = R.string.app_profile_template_save)
                )
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun TextEdit(
    label: String,
    text: String,
    errorHint: String = "",
    isError: Boolean = false,
    onValueChange: (String) -> Unit = {}
) {
    ListItem(headlineContent = {
        val keyboardController = LocalSoftwareKeyboardController.current
        OutlinedTextField(
            value = text,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            suffix = {
                if (errorHint.isNotBlank()) {
                    Text(
                        text = if (isError) errorHint else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            onValueChange = onValueChange
        )
    })
}

private fun isValidTemplateId(id: String): Boolean {
    return Regex("""^([A-Za-z][A-Za-z\d_]*\.)*[A-Za-z][A-Za-z\d_]*$""").matches(id)
}

private fun isTemplateExist(id: String): Boolean {
    return getAppProfileTemplate(id).isNotBlank()
}
