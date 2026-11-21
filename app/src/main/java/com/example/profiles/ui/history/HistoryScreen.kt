package com.example.profiles.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.data.FieldDef
import com.example.profiles.data.Group
import com.example.profiles.data.HistoryType
import com.example.profiles.ui.components.displayName
import com.example.profiles.utils.diffSnapshots
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var revertId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            !state.loaded -> Unit
            state.items.isEmpty() -> Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.history_empty), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.items, key = { it.entry.id }) { item ->
                    HistoryRow(item, state.groups, state.fields, onRevert = { revertId = item.entry.id })
                    HorizontalDivider()
                }
            }
        }
    }

    state.items.find { it.entry.id == revertId }?.let { item ->
        val e = item.entry
        AlertDialog(
            onDismissRequest = { revertId = null },
            title = { Text(stringResource(R.string.revert_title)) },
            text = {
                Text(stringResource(
                    when (e.type) {
                        HistoryType.CREATED -> R.string.revert_created_text
                        HistoryType.UPDATED -> R.string.revert_updated_text
                        HistoryType.DELETED -> R.string.revert_deleted_text
                    },
                    e.personName
                ))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.revert(e.id); revertId = null }) {
                    Text(stringResource(R.string.revert))
                }
            },
            dismissButton = {
                TextButton(onClick = { revertId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    groups: List<Group>,
    fields: List<FieldDef>,
    onRevert: () -> Unit
) {
    val e = item.entry
    val resources = LocalResources.current
    val groupNames = groups.associate { it.id to it.displayName() }
    val fieldNames = fields.associate { it.id to it.displayName() }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    val changes = if (e.type == HistoryType.UPDATED && item.before != null && item.after != null) {
        diffSnapshots(
            item.before, item.after,
            label = resources::getString,
            groupName = { groupNames[it] ?: "—" },
            fieldName = { fieldNames[it] },
            date = { it.format(dateFormat) }
        )
    } else emptyList()

    ListItem(
        leadingContent = {
            Icon(
                when (e.type) {
                    HistoryType.CREATED -> Icons.Outlined.PersonAdd
                    HistoryType.UPDATED -> Icons.Outlined.Edit
                    HistoryType.DELETED -> Icons.Outlined.PersonRemove
                },
                contentDescription = null
            )
        },
        overlineContent = {
            Text(Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
        },
        headlineContent = {
            Text(stringResource(
                when (e.type) {
                    HistoryType.CREATED -> R.string.history_created
                    HistoryType.UPDATED -> R.string.history_updated
                    HistoryType.DELETED -> R.string.history_deleted
                },
                e.personName
            ))
        },
        supportingContent = if (changes.isEmpty()) null else ({
            Column {
                changes.forEach { c ->
                    Text("${c.label}: ${c.old} → ${c.new}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }),
        trailingContent = {
            if (e.revertedAt == null) {
                TextButton(onClick = onRevert) { Text(stringResource(R.string.revert)) }
            } else {
                Text(stringResource(R.string.reverted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}