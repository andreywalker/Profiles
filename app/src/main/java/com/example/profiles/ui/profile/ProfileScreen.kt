package com.example.profiles.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.ui.components.Avatar
import com.example.profiles.ui.components.SectionCard
import com.example.profiles.ui.components.displayName
import com.example.profiles.utils.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.loaded, state.person == null) {
        if (state.loaded && state.person == null) onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.personId) }) {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.delete))
                    }
                }
            )
        }
    ) { padding ->
        val p = state.person
        if (!state.loaded || p == null) return@Scaffold

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(p.photoPath, size = 112.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "${p.firstName} ${p.surname}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                if (p.name != p.firstName) {
                    Text(p.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionCard(stringResource(R.string.section_main_info)) {
                InfoRow(
                    stringResource(R.string.birth_date),
                    p.birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                )
                InfoRow(stringResource(R.string.age), ageOf(p.birthDate).toString())
                InfoRow(stringResource(R.string.group), state.group?.displayName() ?: "")
            }

            SectionCard(stringResource(R.string.section_met)) { Text(p.met) }
            SectionCard(stringResource(R.string.section_subtitle)) { Text(p.subtitle) }

            // Only shown if at least one contact is filled
            if (listOf(p.phone, p.telegram, p.instagram, p.otherMedia).any { it != null }) {
                SectionCard(stringResource(R.string.section_contacts)) {
                    p.phone?.let {
                        InfoRow(stringResource(R.string.phone), it, Icons.Filled.Call) {
                            context.openUri(phoneUri(it), Intent.ACTION_DIAL)
                        }
                    }
                    p.telegram?.let {
                        InfoRow(stringResource(R.string.telegram), it, Icons.AutoMirrored.Filled.Send) {
                            context.openUri(telegramUrl(it))
                        }
                    }
                    p.instagram?.let {
                        InfoRow(stringResource(R.string.instagram), it, Icons.Outlined.PhotoCamera) {
                            context.openUri(instagramUrl(it))
                        }
                    }
                    p.otherMedia?.let {
                        InfoRow(stringResource(R.string.other_media), it, Icons.AutoMirrored.Filled.OpenInNew) {
                            context.openUri(webUrl(it))
                        }
                    }
                }
            }

            if (state.customFields.isNotEmpty()) {
                SectionCard(stringResource(R.string.section_custom_fields)) {
                    state.customFields.forEach { (def, value) ->
                        InfoRow(def.displayName(), value)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val name = state.person?.let { "${it.firstName} ${it.surname}" } ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_profile_title)) },
            text = { Text(stringResource(R.string.delete_profile_text, name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** Label + value, with an optional action icon on the right. */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}