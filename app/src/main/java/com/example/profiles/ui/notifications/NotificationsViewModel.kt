package com.example.profiles.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.profiles.ProfilesApp
import com.example.profiles.data.ProfilesRepository
import com.example.profiles.data.ReminderLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(repo: ProfilesRepository) : ViewModel() {
    val reminders: StateFlow<List<ReminderLog>> = repo.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { NotificationsViewModel((this[APPLICATION_KEY] as ProfilesApp).repository) }
        }
    }
}