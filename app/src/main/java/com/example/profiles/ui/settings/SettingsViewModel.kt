package com.example.profiles.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.*
import com.example.profiles.ui.theme.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.DARK,
    val language: String = "system",
    val groups: List<Group> = emptyList(),
    val fields: List<FieldDef> = emptyList()
)

class SettingsViewModel(private val repo: ProfilesRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repo.observeSetting("theme"),
        repo.observeSetting("language"),
        repo.observeGroups(),
        repo.observeFieldDefs()
    ) { theme, lang, groups, fields ->
        SettingsUiState(
            theme = theme?.let(ThemeMode::valueOf) ?: ThemeMode.DARK,
            language = lang ?: "system",
            groups = groups,
            fields = fields
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = launch { repo.setSetting("theme", mode.name) }

    /** onSaved runs after the value is in the database, so the activity can restart. */
    fun setLanguage(lang: String, onSaved: () -> Unit) = launch {
        repo.setSetting("language", lang)
        onSaved()
    }

    fun addGroup(name: String) = launch { repo.addGroup(name) }
    fun renameGroup(group: Group, name: String) = launch { repo.renameGroup(group, name) }
    fun deleteGroup(group: Group) = launch { repo.deleteGroup(group) }

    fun addField(name: String, type: FieldType) = launch { repo.addField(name, type) }
    fun renameField(def: FieldDef, name: String) = launch { repo.renameField(def, name) }
    fun deleteField(def: FieldDef) = launch { repo.deleteField(def) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel((this[APPLICATION_KEY] as ProfilesApp).repository) }
        }
    }
}