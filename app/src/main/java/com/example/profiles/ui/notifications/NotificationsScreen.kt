package com.example.profiles.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
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
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

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
        if (reminders.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_notifications), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(reminders, key = { "${it.personId}-${it.year}-${it.daysBefore}" }) { r ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenProfile(r.personId) },
                        overlineContent = {
                            Text(Instant.ofEpochMilli(r.sentAt).atZone(ZoneId.systemDefault()).format(formatter))
                        },
                        headlineContent = { Text(r.title) },
                        supportingContent = { Text(r.text) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}