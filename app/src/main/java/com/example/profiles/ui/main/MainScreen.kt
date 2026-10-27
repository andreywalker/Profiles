package com.example.profiles.ui.main


import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.profiles.R
import com.example.profiles.notifications.NotificationHelper
import com.example.profiles.utils.ageOf
import com.example.profiles.ui.components.Avatar
import com.example.profiles.ui.components.GroupNameDialog
import com.example.profiles.ui.components.displayName

@Composable
fun MainScreen(
    onPersonClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* nothing to do: if denied, reminders still appear in history */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !NotificationHelper.hasPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    MainContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onGroupSelected = viewModel::onGroupSelected,
        onPersonClick = onPersonClick,
        onAddClick = onAddClick,
        onSettingsClick = onSettingsClick,
        onAddGroup = viewModel::addGroup,
        onNotificationsClick = onNotificationsClick
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    state: MainUiState,
    onQueryChange: (String) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onPersonClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onAddGroup: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    // survives rotation thanks to rememberSaveable
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var showNewGroupDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Outlined.Notifications, stringResource(R.string.notifications))
                    }
                },
                title = {
                    if (searchOpen) {
                        SearchField(state.query, onQueryChange)
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (searchOpen) {
                        IconButton(onClick = { searchOpen = false; onQueryChange("") }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close_search))
                        }
                    } else {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.search))
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_person))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GroupTabs(state, onGroupSelected, onAddGroupClick = { showNewGroupDialog = true })
            if (state.people.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(state.people, key = { it.person.id }) { item ->
                        PersonRow(item, onClick = { onPersonClick(item.person.id) })
                    }
                }
            }
        }
    }
    if (showNewGroupDialog) {
        GroupNameDialog(
            title = stringResource(R.string.new_group),
            confirmText = stringResource(R.string.create),
            existingNames = state.groups.map { it.displayName() },
            onDismiss = { showNewGroupDialog = false },
            onConfirm = onAddGroup
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupTabs(
    state: MainUiState,
    onGroupSelected: (Long?) -> Unit,
    onAddGroupClick: () -> Unit
) {
    val selectedIndex = state.groups.indexOfFirst { it.id == state.selectedGroupId } + 1
    PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
        Tab(
            selected = state.selectedGroupId == null,
            onClick = { onGroupSelected(null) },
            text = { Text(stringResource(R.string.all_groups)) }
        )
        state.groups.forEach { group ->
            Tab(
                selected = group.id == state.selectedGroupId,
                onClick = { onGroupSelected(group.id) },
                text = { Text(group.displayName()) }
            )
        }
        // never "selected": it just opens the dialog
        Tab(
            selected = false,
            onClick = onAddGroupClick,
            icon = { Icon(Icons.Filled.Add, stringResource(R.string.new_group)) }
        )
    }
}

@Composable
private fun PersonRow(item: PersonItem, onClick: () -> Unit) {
    val p = item.person
    val supporting = listOfNotNull(
        p.subtitle.takeIf { it.isNotBlank() },
        item.group?.displayName()
    ).joinToString(" · ")

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Avatar(p.photoPath) },
        headlineContent = { Text("${p.firstName} ${p.surname}") },
        supportingContent = { Text(supporting) },
        trailingContent = {
            Text(ageOf(p.birthDate).toString(), style = MaterialTheme.typography.titleMedium)
        }
    )
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Group, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_profiles), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.no_profiles_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}