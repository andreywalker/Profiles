package com.example.profiles.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.R
import com.example.profiles.data.FieldDef
import com.example.profiles.data.Group
import com.example.profiles.data.HistoryEntry
import com.example.profiles.data.PersonSnapshot
import com.example.profiles.data.PhotoStorage
import com.example.profiles.data.ProfilesRepository
import com.example.profiles.notifications.BirthdayWorker
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** One history entry with its before/after snapshots already parsed. */
data class HistoryItem(
    val entry: HistoryEntry,
    val before: PersonSnapshot?,
    val after: PersonSnapshot?
)

data class HistoryUiState(
    val loaded: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val groups: List<Group> = emptyList(),
    val fields: List<FieldDef> = emptyList()
)

class HistoryViewModel(
    private val repo: ProfilesRepository,
    private val photos: PhotoStorage,
    private val messenger: Messenger,
    private val onDataChanged: () -> Unit      // re-checks birthday reminders after a revert
) : ViewModel() {

    private val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

    val state: StateFlow<HistoryUiState> = combine(
        repo.observeHistory(since),
        repo.observeGroups(),
        repo.observeFieldDefs()
    ) { entries, groups, fields ->
        HistoryUiState(
            loaded = true,
            items = entries.map { e ->
                HistoryItem(
                    entry = e,
                    before = e.before?.let(PersonSnapshot::fromJson),
                    after = e.after?.let(PersonSnapshot::fromJson)
                )
            },
            groups = groups,
            fields = fields
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        // Drop entries older than 30 days, then photos nothing needs anymore
        viewModelScope.launch {
            repo.purgeHistory(since)
            val keep = repo.referencedPhotos()
            withContext(Dispatchers.IO) { photos.keepOnly(keep) }
        }
    }

    fun revert(entryId: Long) {
        viewModelScope.launch {
            if (repo.revert(entryId)) {
                onDataChanged()
                messenger.show(UiMessage(R.string.msg_reverted))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                HistoryViewModel(
                    repo = app.repository,
                    photos = app.photos,
                    messenger = app.messenger,
                    onDataChanged = { BirthdayWorker.runNow(app) }
                )
            }
        }
    }
}