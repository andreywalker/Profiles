package com.example.profiles.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.R
import com.example.profiles.data.ProfilesRepository
import com.example.profiles.data.ReminderLog
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One reminder plus whether its person still exists. */
data class ReminderItem(val log: ReminderLog, val personExists: Boolean)

data class NotificationsUiState(
    val loaded: Boolean = false,
    val items: List<ReminderItem> = emptyList()
)

class NotificationsViewModel(
    repo: ProfilesRepository,
    private val messenger: Messenger
) : ViewModel() {

    // History keeps reminders of deleted people; they are marked instead of hidden.
    // After a Revert in Settings -> History the person exists again and the mark disappears.
    val state: StateFlow<NotificationsUiState> =
        combine(repo.observeReminders(), repo.observePeople()) { logs, people ->
            val ids = people.mapTo(HashSet()) { it.id }
            NotificationsUiState(
                loaded = true,
                items = logs.map { ReminderItem(it, it.personId in ids) }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    /** Tap on a reminder whose profile no longer exists. */
    fun onDeletedPersonClick() {
        messenger.show(UiMessage(R.string.msg_reminder_person_deleted))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ProfilesApp
                NotificationsViewModel(app.repository, app.messenger)
            }
        }
    }
}
