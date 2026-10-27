package com.example.profiles.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.data.FieldType
import com.example.profiles.ui.components.GroupNameDialog
import com.example.profiles.ui.components.displayName
import com.example.profiles.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    // which dialog is open (null = none); saveable so they survive rotation
    var showAddGroup by rememberSaveable { mutableStateOf(false) }
    var renameGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddField by rememberSaveable { mutableStateOf(false) }
    var renameFieldId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteFieldId by rememberSaveable { mutableStateOf<Long?>(null) }

    val groupNames = state.groups.map { it.displayName() }
    val fieldNames = state.fields.map { it.displayName() }
    val languages = listOf(
        "system" to stringResource(R.string.language_system),
        "en" to "English",
        "ru" to "Русский"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {

            item { SectionHeader(stringResource(R.string.theme)) }
            items(ThemeMode.entries) { mode ->
                RadioRow(
                    label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        }
                    ),
                    selected = state.theme == mode,
                    onClick = { viewModel.setTheme(mode) }
                )
            }

            item { SectionHeader(stringResource(R.string.language)) }
            items(languages) { (code, label) ->
                RadioRow(
                    label = label,
                    selected = state.language == code,
                    onClick = {
                        if (state.language != code) {
                            viewModel.setLanguage(code) { activity?.recreate() }
                        }
                    }
                )
            }

            item { SectionHeader(stringResource(R.string.groups)) }
            items(state.groups, key = { "g${it.id}" }) { group ->
                ListItem(
                    headlineContent = { Text(group.displayName()) },
                    trailingContent = {
                        if (!group.isDefault) {     // "No group" can't be renamed or deleted
                            Row {
                                IconButton(onClick = { renameGroupId = group.id }) {
                                    Icon(Icons.Outlined.Edit, stringResource(R.string.rename))
                                }
                                IconButton(onClick = { deleteGroupId = group.id }) {
                                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                )
            }
            item { AddRow(stringResource(R.string.new_group)) { showAddGroup = true } }

            item { SectionHeader(stringResource(R.string.section_custom_fields)) }
            items(state.fields, key = { "f${it.id}" }) { field ->
                ListItem(
                    headlineContent = { Text(field.displayName()) },
                    supportingContent = { Text(typeLabel(field.type)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renameFieldId = field.id }) {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.rename))
                            }
                            if (field.deletable) {   // Facts can't be deleted
                                IconButton(onClick = { deleteFieldId = field.id }) {
                                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                )
            }
            item { AddRow(stringResource(R.string.new_field)) { showAddField = true } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ----- Dialogs -----

    if (showAddGroup) {
        GroupNameDialog(
            title = stringResource(R.string.new_group),
            confirmText = stringResource(R.string.create),
            existingNames = groupNames,
            onDismiss = { showAddGroup = false },
            onConfirm = viewModel::addGroup
        )
    }

    state.groups.find { it.id == renameGroupId }?.let { group ->
        GroupNameDialog(
            title = stringResource(R.string.rename_group),
            confirmText = stringResource(R.string.save),
            existingNames = groupNames,
            initial = group.name,
            onDismiss = { renameGroupId = null },
            onConfirm = { viewModel.renameGroup(group, it) }
        )
    }

    state.groups.find { it.id == deleteGroupId }?.let { group ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_group_title, group.name),
            text = stringResource(R.string.delete_group_text),
            onDismiss = { deleteGroupId = null },
            onConfirm = { viewModel.deleteGroup(group) }
        )
    }

    if (showAddField) {
        FieldDialog(
            title = stringResource(R.string.new_field),
            confirmText = stringResource(R.string.create),
            existingNames = fieldNames,
            allowTypeChoice = true,
            onDismiss = { showAddField = false },
            onConfirm = viewModel::addField
        )
    }

    state.fields.find { it.id == renameFieldId }?.let { field ->
        FieldDialog(
            title = stringResource(R.string.rename_field),
            confirmText = stringResource(R.string.save),
            existingNames = fieldNames,
            initial = field.displayName(),
            allowTypeChoice = false,
            onDismiss = { renameFieldId = null },
            onConfirm = { name, _ -> viewModel.renameField(field, name) }
        )
    }

    state.fields.find { it.id == deleteFieldId }?.let { field ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_field_title, field.displayName()),
            text = stringResource(R.string.delete_field_text),
            onDismiss = { deleteFieldId = null },
            onConfirm = { viewModel.deleteField(field) }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        headlineContent = { Text(label) }
    )
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun typeLabel(type: FieldType): String = when (type) {
    FieldType.TEXT -> stringResource(R.string.type_text)
}

@Composable
private fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun FieldDialog(
    title: String,
    confirmText: String,
    existingNames: List<String>,
    allowTypeChoice: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, FieldType) -> Unit,
    initial: String = ""
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    var type by rememberSaveable { mutableStateOf(FieldType.TEXT) }
    val trimmed = name.trim()
    val duplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) && it != initial }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(R.string.field_exists)) }) else null
                )
                if (allowTypeChoice) {
                    Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp))
                    FieldType.entries.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(selected = type == t, onClick = { type = t }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = type == t, onClick = null)
                            Text(typeLabel(t), Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed, type); onDismiss() },
                enabled = trimmed.isNotEmpty() && !duplicate
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}