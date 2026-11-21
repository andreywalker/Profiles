package com.example.profiles.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.R
import com.example.profiles.data.*
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class HistoryItem(val entry: HistoryEntry, val before: PersonSnapshot?, val after: PersonSnapshot?)

data class HistoryUiState(
    val loaded: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val groups: List<Group> = emptyList(),
    val fields: List<FieldDef> = emptyList()
)

class HistoryViewModel(
    private val repo: ProfilesRepository,
    private val photos: PhotoStorage,
    private val messenger: Messenger
) : ViewModel() {

    private val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

    val state: StateFlow<HistoryUiState> = combine(
        repo.observeHistory(since), repo.observeGroups(), repo.observeFieldDefs()
    ) { entries, groups, fields ->
        HistoryUiState(
            loaded = true,
            items = entries.map { e ->
                HistoryItem(e, e.before?.let(PersonSnapshot::fromJson), e.after?.let(PersonSnapshot::fromJson))
            },
            groups = groups,
            fields = fields
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        // drop entries older than 30 days and photos nothing needs anymore
        viewModelScope.launch {
            repo.purgeHistory(since)
            val keep = repo.referencedPhotos()
            withContext(Dispatchers.IO) { photos.keepOnly(keep) }
        }
    }

    fun revert(entryId: Long) {
        viewModelScope.launch {
            if (repo.revert(entryId)) messenger.show(UiMessage(R.string.msg_reverted))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                HistoryViewModel(app.repository, app.photos, app.messenger)
            }
        }
    }
}