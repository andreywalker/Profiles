package com.example.profiles.utils

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UiMessage(
    val text: Int,                        // @StringRes, or @PluralsRes when quantity is set
    val args: List<Any> = emptyList(),
    val quantity: Int? = null,            // ← new
    @StringRes val action: Int? = null,
    val onAction: (suspend () -> Unit)? = null,
    val onDismiss: (suspend () -> Unit)? = null
)

/** ViewModels post messages here; MainActivity shows them as snackbars. */
class Messenger {
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun show(message: UiMessage) {
        _messages.tryEmit(message)
    }
}