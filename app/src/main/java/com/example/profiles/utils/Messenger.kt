package com.example.profiles.utils

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UiMessage(
    @StringRes val text: Int,
    val args: List<Any> = emptyList(),
    @StringRes val action: Int? = null,
    val onAction: (suspend () -> Unit)? = null,     // e.g. Undo
    val onDismiss: (suspend () -> Unit)? = null     // runs if the action wasn't used
)

/** ViewModels post messages here; MainActivity shows them as snackbars. */
class Messenger {
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun show(message: UiMessage) {
        _messages.tryEmit(message)
    }
}