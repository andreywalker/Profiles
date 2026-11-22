package com.example.profiles.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.ui.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            !state.loaded -> Unit    // still loading: show nothing

            state.items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.NotificationsNone,
                title = stringResource(R.string.no_notifications),
                hint = stringResource(R.string.no_notifications_hint),
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(
                    state.items,
                    key = { "${it.log.personId}-${it.log.birthday}-${it.log.daysBefore}" }
                ) { item ->
                    ReminderRow(
                        item = item,
                        onClick = {
                            if (item.personExists) onOpenProfile(item.log.personId)
                            else viewModel.onDeletedPersonClick()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(item: ReminderItem, onClick: () -> Unit) {
    val r = item.log
    val deleted = !item.personExists
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = {
            Text(
                Instant.ofEpochMilli(r.sentAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
            )
        },
        headlineContent = {
            Text(
                r.title,
                textDecoration = if (deleted) TextDecoration.LineThrough else null
            )
        },
        supportingContent = { Text(r.text) },
        trailingContent = if (deleted) ({
            Text(
                stringResource(R.string.reminder_profile_deleted),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }) else null,
        colors = if (deleted) ListItemDefaults.colors(
            headlineColor = muted,
            supportingColor = muted
        ) else ListItemDefaults.colors()
    )
}
