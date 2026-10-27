package com.example.profiles.ui.main


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.Group
import com.example.profiles.data.Person
import com.example.profiles.data.ProfilesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PersonItem(val person: Person, val group: Group?)

data class MainUiState(
    val groups: List<Group> = emptyList(),
    val people: List<PersonItem> = emptyList(),
    val selectedGroupId: Long? = null,      // null = "All"
    val query: String = ""
)

class MainViewModel(private val repo: ProfilesRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedGroupId = MutableStateFlow<Long?>(null)

    val state: StateFlow<MainUiState> = combine(
        repo.observePeople(), repo.observeGroups(), query, selectedGroupId
    ) { people, groups, q, selected ->
        val groupsById = groups.associateBy { it.id }
        // if the selected group was deleted, fall back to "All"
        val sel = selected?.takeIf { it in groupsById }
        val text = q.trim()

        val items = people
            .filter { sel == null || it.groupId == sel }
            .filter { p ->
                text.isEmpty() || listOf(p.firstName, p.surname, p.name)
                    .any { it.contains(text, ignoreCase = true) }
            }
            .map { PersonItem(it, groupsById[it.groupId]) }

        MainUiState(groups, items, sel, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onGroupSelected(id: Long?) { selectedGroupId.value = id }
    fun addGroup(name: String) {
        viewModelScope.launch {
            val id = repo.addGroup(name)
            selectedGroupId.value = id
        }
    }
    companion object {
        val Factory = viewModelFactory {
            initializer {
                MainViewModel((this[APPLICATION_KEY] as ProfilesApp).repository)
            }
        }
    }
}