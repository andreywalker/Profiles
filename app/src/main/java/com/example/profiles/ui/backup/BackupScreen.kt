package com.example.profiles.ui.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.data.remote.FieldDefDto
import com.example.profiles.ui.components.EmptyState
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load, enabled = state !is BackupUiState.Loading) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)

        when (val s = state) {
            // Loading
            BackupUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            // Error: server down, HTTP error with code, or other failure
            is BackupUiState.Error -> EmptyState(
                icon = if (s.serverDown) Icons.Outlined.CloudOff else Icons.Outlined.SyncProblem,
                title = stringResource(
                    if (s.serverDown) R.string.sync_server_down else R.string.backup_load_error
                ),
                hint = when {
                    s.serverDown -> stringResource(R.string.backup_server_down_hint)
                    s.code != null -> stringResource(
                        R.string.error_code, s.code, s.message?.let { "— $it" }.orEmpty()
                    )
                    else -> s.message.orEmpty()
                },
                actionLabel = stringResource(R.string.retry),
                onAction = viewModel::load,
                modifier = modifier
            )

            // Success, but nothing backed up yet
            is BackupUiState.Success -> if (s.people.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.CloudQueue,
                    title = stringResource(R.string.backup_empty),
                    hint = stringResource(R.string.backup_empty_hint),
                    actionLabel = stringResource(R.string.sync_now),
                    onAction = viewModel::syncAndReload,
                    modifier = modifier
                )
            } else {
                // Success: the data received from the server
                LazyColumn(modifier.fillMaxSize()) {
                    item(key = "header") { BackupHeader(s.syncedAt, s.people.size) }
                    items(s.people, key = { it.person.id }) { item ->
                        RemotePersonRow(item)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupHeader(syncedAt: OffsetDateTime?, count: Int) {
    val time = syncedAt
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            if (time != null) stringResource(R.string.backup_last, time) else stringResource(R.string.never_synced),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            pluralStringResource(R.plurals.backup_people, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemotePersonRow(item: RemotePerson) {
    val p = item.person
    val groupName = item.group?.let { if (it.isDefault) stringResource(R.string.no_group) else it.name }
    val birthDate = runCatching {
        LocalDate.parse(p.birthDate).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(p.birthDate)

    ListItem(
        headlineContent = { Text("${p.firstName} ${p.surname}") },
        supportingContent = {
            Column {
                Text(listOfNotNull(p.subtitle.takeIf { it.isNotBlank() }, groupName).joinToString(" · "))
                item.values.forEach { (def, value) ->
                    Text(
                        "${fieldLabel(def)}: $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        trailingContent = { Text(birthDate, style = MaterialTheme.typography.labelMedium) }
    )
}

/** Built-in fields are shown translated, like in the rest of the app. */
@Composable
private fun fieldLabel(def: FieldDefDto): String = when (def.builtInKey) {
    "favorite_color" -> stringResource(R.string.favorite_color)
    "wishlist" -> stringResource(R.string.wishlist)
    "facts" -> stringResource(R.string.facts)
    else -> def.name
}
