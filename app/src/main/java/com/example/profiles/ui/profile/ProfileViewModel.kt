package com.example.profiles.ui.profile


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.R
import com.example.profiles.data.*
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loaded: Boolean = false,
    val person: Person? = null,
    val group: Group? = null,
    val customFields: List<Pair<FieldDef, String>> = emptyList()   // only filled ones
)

class ProfileViewModel(
    savedState: SavedStateHandle,
    private val repo: ProfilesRepository,
    private val photos: PhotoStorage,
    private val messenger: Messenger
) : ViewModel() {

    val personId: Long = checkNotNull(savedState["id"])

    val state: StateFlow<ProfileUiState> = combine(
        repo.observePerson(personId),
        repo.observeGroups(),
        repo.observeFieldDefs(),
        repo.observeFieldValues(personId)
    ) { person, groups, defs, values ->
        val byField = values.associate { it.fieldId to it.value }
        ProfileUiState(
            loaded = true,
            person = person,
            group = groups.firstOrNull { it.id == person?.groupId },
            customFields = defs.mapNotNull { d -> byField[d.id]?.let { d to it } }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())



    fun delete() {
        viewModelScope.launch {
            val name = state.value.person?.let { "${it.firstName} ${it.surname}" } ?: return@launch
            val entryId = repo.deletePerson(personId) ?: return@launch
            messenger.show(
                UiMessage(
                text = R.string.msg_profile_deleted,
                args = listOf(name),
                action = R.string.undo,
                onAction = { repo.revert(entryId) }      // same as Revert in History
            ))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                ProfileViewModel(createSavedStateHandle(), app.repository, app.photos, app.messenger)
            }
        }
    }
}