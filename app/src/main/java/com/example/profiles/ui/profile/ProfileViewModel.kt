package com.example.profiles.ui.profile


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.*
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
    private val photos: PhotoStorage
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

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun delete() {
        viewModelScope.launch {
            photos.delete(state.value.person?.photoPath)
            repo.deletePerson(personId)          // custom values go with it (CASCADE)
            _deleted.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                ProfileViewModel(createSavedStateHandle(), app.repository, app.photos)
            }
        }
    }
}