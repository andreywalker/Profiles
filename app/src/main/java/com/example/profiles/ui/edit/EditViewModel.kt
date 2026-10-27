package com.example.profiles.ui.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class EditUiState(
    val loaded: Boolean = false,
    val title: String = "",                       // "Name Surname" heading when editing
    val firstName: String = "",
    val surname: String = "",
    val name: String = "",
    val birthDate: LocalDate? = null,
    val groupId: Long = 0,
    val met: String = "",
    val subtitle: String = "",
    val photoPath: String? = null,
    val phone: String = "",
    val telegram: String = "",
    val instagram: String = "",
    val otherMedia: String = "",
    val customValues: Map<Long, String> = emptyMap(),
    val showErrors: Boolean = false,
    val saved: Boolean = false
) {
    val isValid: Boolean
        get() = listOf(firstName, surname, name, met, subtitle).all { it.isNotBlank() } &&
                birthDate != null
}

class EditViewModel(
    savedState: SavedStateHandle,
    private val repo: ProfilesRepository,
    private val photos: PhotoStorage
) : ViewModel() {

    private val personId: Long = savedState["id"] ?: -1L
    val isNew = personId == -1L

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    val groups = repo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val fieldDefs = repo.observeFieldDefs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // runs once per ViewModel, so not again on rotation
        viewModelScope.launch {
            if (isNew) {
                _state.update { it.copy(groupId = repo.defaultGroupId(), loaded = true) }
            } else {
                val p = repo.getPerson(personId) ?: return@launch
                val values = repo.getFieldValues(personId).associate { it.fieldId to it.value }
                _state.value = EditUiState(
                    loaded = true,
                    title = "${p.firstName} ${p.surname}",
                    firstName = p.firstName, surname = p.surname, name = p.name,
                    birthDate = p.birthDate, groupId = p.groupId,
                    met = p.met, subtitle = p.subtitle, photoPath = p.photoPath,
                    phone = p.phone.orEmpty(), telegram = p.telegram.orEmpty(),
                    instagram = p.instagram.orEmpty(), otherMedia = p.otherMedia.orEmpty(),
                    customValues = values
                )
            }
        }
    }

    /** Generic updater for simple fields: vm.update { it.copy(surname = v) } */
    fun update(change: (EditUiState) -> EditUiState) = _state.update(change)

    /** "Name" follows First name until the user changes it. */
    fun onFirstNameChange(value: String) = _state.update {
        val follows = it.name.isBlank() || it.name == it.firstName
        it.copy(firstName = value, name = if (follows) value else it.name)
    }

    fun onCustomValueChange(fieldId: Long, value: String) = _state.update {
        it.copy(customValues = it.customValues + (fieldId to value))
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { photos.save(uri) }
            _state.update { it.copy(photoPath = path) }
        }
    }

    fun onRemovePhoto() = _state.update { it.copy(photoPath = null) }

    fun save() {
        val s = _state.value
        if (!s.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val person = Person(
                id = if (isNew) 0 else personId,
                firstName = s.firstName.trim(),
                surname = s.surname.trim(),
                name = s.name.trim(),
                birthDate = s.birthDate!!,
                groupId = s.groupId,
                met = s.met.trim(),
                subtitle = s.subtitle.trim(),
                photoPath = s.photoPath,
                phone = s.phone.orNull(),
                telegram = s.telegram.orNull(),
                instagram = s.instagram.orNull(),
                otherMedia = s.otherMedia.orNull()
            )
            repo.savePerson(person, s.customValues)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun String.orNull() = trim().ifBlank { null }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                EditViewModel(createSavedStateHandle(), app.repository, app.photos)
            }
        }
    }
}