package com.example.profiles.ui.edit


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.ui.components.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    onDone: () -> Unit,
    viewModel: EditViewModel = viewModel(factory = EditViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNewGroupDialog by rememberSaveable { mutableStateOf(false) }
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val fieldDefs by viewModel.fieldDefs.collectAsStateWithLifecycle()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    // go back once saved
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) stringResource(R.string.new_profile) else state.title) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(onClick = viewModel::save, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        if (!state.loaded) return@Scaffold
        val err = state.showErrors

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(state.photoPath, size = 112.dp)
                Row {
                    TextButton(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(stringResource(if (state.photoPath == null) R.string.add_photo else R.string.change_photo))
                    }
                    if (state.photoPath != null) {
                        TextButton(onClick = viewModel::onRemovePhoto) { Text(stringResource(R.string.remove_photo)) }
                    }
                }
            }

            SectionCard(stringResource(R.string.section_main_info)) {
                FormField(state.name, { v -> viewModel.update { it.copy(name = v) } },
                    stringResource(R.string.called_name), required = true, showError = err)
                FormField(state.firstName, viewModel::onFirstNameChange,
                    stringResource(R.string.first_name), required = true, showError = err)
                FormField(state.surname, { v -> viewModel.update { it.copy(surname = v) } },
                    stringResource(R.string.surname), required = true, showError = err)


                ClickableField(
                    value = state.birthDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "",
                    label = stringResource(R.string.birth_date),
                    onClick = { showDatePicker = true },
                    trailingIcon = Icons.Outlined.CalendarMonth,
                    isError = err && state.birthDate == null
                )

                Box {
                    ClickableField(
                        value = groups.firstOrNull { it.id == state.groupId }?.displayName() ?: "",
                        label = stringResource(R.string.group),
                        onClick = { showGroupMenu = true },
                        trailingIcon = Icons.Filled.ArrowDropDown
                    )
                    DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.displayName()) },
                                onClick = {
                                    viewModel.update { it.copy(groupId = g.id) }
                                    showGroupMenu = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.new_group)) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = {
                                showGroupMenu = false
                                showNewGroupDialog = true
                            }
                        )
                    }
                }
            }

            SectionCard(stringResource(R.string.section_met)) {
                FormField(state.met, { v -> viewModel.update { it.copy(met = v) } },
                    stringResource(R.string.met_label), required = true, showError = err, multiline = true)
            }

            SectionCard(stringResource(R.string.section_subtitle)) {
                FormField(state.subtitle, { v -> viewModel.update { it.copy(subtitle = v) } },
                    stringResource(R.string.subtitle_label), required = true, showError = err)
            }

            SectionCard(stringResource(R.string.section_contacts)) {
                FormField(state.phone, { v -> viewModel.update { it.copy(phone = v) } },
                    stringResource(R.string.phone), keyboardType = KeyboardType.Phone)
                FormField(state.telegram, { v -> viewModel.update { it.copy(telegram = v) } },
                    stringResource(R.string.telegram))
                FormField(state.instagram, { v -> viewModel.update { it.copy(instagram = v) } },
                    stringResource(R.string.instagram))
                FormField(state.otherMedia, { v -> viewModel.update { it.copy(otherMedia = v) } },
                    stringResource(R.string.other_media), keyboardType = KeyboardType.Uri)
            }

            SectionCard(stringResource(R.string.section_custom_fields)) {
                // DAO order puts Facts last
                fieldDefs.forEach { def ->
                    FormField(
                        value = state.customValues[def.id] ?: "",
                        onValueChange = { viewModel.onCustomValueChange(def.id, it) },
                        label = def.displayName(),
                        multiline = !def.deletable || def.builtInKey == "wishlist"
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        BirthDatePicker(
            initial = state.birthDate,
            onDismiss = { showDatePicker = false },
            onPicked = { date -> viewModel.update { it.copy(birthDate = date) } }
        )
    }
    if (showNewGroupDialog) {
        GroupNameDialog(
            title = stringResource(R.string.new_group),
            confirmText = stringResource(R.string.create),
            existingNames = groups.map { it.displayName() },
            onDismiss = { showNewGroupDialog = false },
            onConfirm = viewModel::createGroup
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(initial: LocalDate?, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    ) { DatePicker(state = pickerState) }
}