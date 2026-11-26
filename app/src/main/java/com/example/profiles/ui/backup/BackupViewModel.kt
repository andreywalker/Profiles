package com.example.profiles.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.ApiResult
import com.example.profiles.data.BackupRepository
import com.example.profiles.data.remote.BackupDto
import com.example.profiles.data.remote.FieldDefDto
import com.example.profiles.data.remote.GroupDto
import com.example.profiles.data.remote.PersonDto
import com.example.profiles.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/** One person from the server, with its group and filled custom fields resolved. */
data class RemotePerson(
    val person: PersonDto,
    val group: GroupDto?,
    val values: List<Pair<FieldDefDto, String>>
)

sealed interface BackupUiState {
    data object Loading : BackupUiState
    data class Success(val syncedAt: OffsetDateTime?, val people: List<RemotePerson>) : BackupUiState
    data class Error(val serverDown: Boolean, val code: Int? = null, val message: String? = null) : BackupUiState
}

class BackupViewModel(
    private val backup: BackupRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { fetch() }
    }

    /** "Sync now" on the empty state: upload, then show what the server has. */
    fun syncAndReload() {
        viewModelScope.launch {
            _state.value = BackupUiState.Loading
            syncManager.sync(manual = true)
            fetch()
        }
    }

    private suspend fun fetch() {
        _state.value = BackupUiState.Loading
        _state.value = when (val result = backup.download()) {
            is ApiResult.Success -> result.data.toUiState()
            ApiResult.Unreachable -> BackupUiState.Error(serverDown = true)
            is ApiResult.HttpError -> BackupUiState.Error(false, result.code, result.message)
            is ApiResult.Failure -> BackupUiState.Error(false, null, result.message)
        }
    }

    private fun BackupDto.toUiState(): BackupUiState.Success {
        val groupsById = groups.associateBy { it.id }
        // same order as in the app: deletable fields by position, Facts last
        val orderedFields = fields.sortedWith(
            compareByDescending<FieldDefDto> { it.deletable }.thenBy { it.position }
        )
        val people = persons
            .sortedBy { it.firstName.lowercase() }
            .map { p ->
                RemotePerson(
                    person = p,
                    group = groupsById[p.groupId],
                    values = orderedFields.mapNotNull { def -> p.values[def.id.toString()]?.let { def to it } }
                )
            }
        val time = syncedAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        return BackupUiState.Success(time, people)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                BackupViewModel(app.backupRepository, app.syncManager)
            }
        }
    }
}
