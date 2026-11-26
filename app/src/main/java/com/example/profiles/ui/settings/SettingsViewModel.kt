package com.example.profiles.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.*
import com.example.profiles.ui.theme.ThemeMode
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.profiles.R

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.DARK,
    val language: String = "system",
    val groups: List<Group> = emptyList(),
    val fields: List<FieldDef> = emptyList(),
    val serverUrl: String = BackupRepository.DEFAULT_SERVER_URL,
    val lastSync: Long? = null
)

class SettingsViewModel(
    private val repo: ProfilesRepository,
    private val messenger: Messenger
) : ViewModel()  {
    private val server = combine(
        repo.observeSetting(BackupRepository.KEY_SERVER_URL),
        repo.observeSetting(BackupRepository.KEY_LAST_SYNC)
    ) { url, last -> (url ?: BackupRepository.DEFAULT_SERVER_URL) to last?.toLongOrNull() }

    val state: StateFlow<SettingsUiState> = combine(
        repo.observeSetting("theme"),
        repo.observeSetting("language"),
        repo.observeGroups(),
        repo.observeFieldDefs(),
        server
    ) { theme, lang, groups, fields, srv ->
        SettingsUiState(
            theme = theme?.let(ThemeMode::valueOf) ?: ThemeMode.DARK,
            language = lang ?: "system",
            groups = groups,
            fields = fields,
            serverUrl = srv.first,
            lastSync = srv.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setServerUrl(url: String) = launch { repo.setSetting(BackupRepository.KEY_SERVER_URL, url) }

    fun setTheme(mode: ThemeMode) = launch { repo.setSetting("theme", mode.name) }

    /** onSaved runs after the value is in the database, so the activity can restart. */
    fun setLanguage(lang: String, onSaved: () -> Unit) = launch {
        repo.setSetting("language", lang)
        onSaved()
    }

    fun addGroup(name: String) = launch { repo.addGroup(name) }
    fun renameGroup(group: Group, name: String) = launch { repo.renameGroup(group, name) }
    fun deleteGroup(group: Group) = launch {
        val deleted = repo.deleteGroup(group) ?: return@launch
        val moved = deleted.memberIds.size
        messenger.show(
            if (moved == 0) UiMessage(
                text = R.string.msg_group_deleted,
                args = listOf(group.name),
                action = R.string.undo,
                onAction = { repo.restoreGroup(deleted) }
            ) else UiMessage(
                text = R.plurals.msg_group_deleted_moved,
                args = listOf(group.name, moved),
                quantity = moved,
                action = R.string.undo,
                onAction = { repo.restoreGroup(deleted) }
            )
        )
    }



    fun addField(name: String, type: FieldType) = launch { repo.addField(name, type) }
    fun renameField(def: FieldDef, name: String) = launch { repo.renameField(def, name) }
    /** displayName is passed from the UI, so built-in fields show translated. */
    fun deleteField(def: FieldDef, displayName: String) = launch {
        val deleted = repo.deleteField(def) ?: return@launch
        messenger.show(UiMessage(
            text = R.string.msg_field_deleted,
            args = listOf(displayName),
            action = R.string.undo,
            onAction = { repo.restoreField(deleted) }
        ))
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel((this[APPLICATION_KEY] as ProfilesApp).repository, (this[APPLICATION_KEY] as ProfilesApp).messenger) }
        }
    }
}