package com.example.profiles.sync

import com.example.profiles.R
import com.example.profiles.data.ApiResult
import com.example.profiles.data.AppDb
import com.example.profiles.data.BackupRepository
import com.example.profiles.utils.Messenger
import com.example.profiles.utils.UiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

sealed interface SyncState {
    data object Idle : SyncState                          // not synced yet, or data changed since
    data object Syncing : SyncState
    data class Synced(val at: Long) : SyncState
    data class Error(val serverDown: Boolean) : SyncState
}

/**
 * One shared sync state for the whole app: the top-bar button and the
 * 2-hour background worker both go through here.
 */
class SyncManager(
    private val backup: BackupRepository,
    private val messenger: Messenger,
    db: AppDb,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val mutex = Mutex()     // never two syncs at once

    init {
        // Any local change after a sync means the server copy is outdated again
        scope.launch {
            db.invalidationTracker
                .createFlow("persons", "person_groups", "field_defs", "field_values", emitInitialState = false)
                .collect { if (_state.value is SyncState.Synced) _state.value = SyncState.Idle }
        }
    }

    /** For the top-bar button. Runs in the app scope, so leaving the screen doesn't cancel it. */
    fun requestSync() {
        scope.launch { sync(manual = true) }
    }

    /** 1) check the server, 2) if alive, upload. Snackbars only for manual syncs. */
    suspend fun sync(manual: Boolean): SyncState {
        if (!mutex.tryLock()) return _state.value             // already syncing
        try {
            _state.value = SyncState.Syncing

            val result = when (val check = backup.checkServer()) {
                is ApiResult.Success -> backup.upload()
                else -> check                                 // server down or unhealthy: stop here
            }

            val newState = when (result) {
                is ApiResult.Success -> {
                    val now = System.currentTimeMillis()
                    backup.saveLastSync(now)
                    SyncState.Synced(now)
                }
                ApiResult.Unreachable -> {
                    if (manual) messenger.show(UiMessage(R.string.sync_server_down))
                    SyncState.Error(serverDown = true)
                }
                is ApiResult.HttpError -> {
                    if (manual) messenger.show(UiMessage(
                        R.string.sync_http_error,
                        listOf(result.code, result.message?.let { "— $it" }.orEmpty())
                    ))
                    SyncState.Error(serverDown = false)
                }
                is ApiResult.Failure -> {
                    if (manual) messenger.show(UiMessage(R.string.sync_failed, listOf(result.message)))
                    SyncState.Error(serverDown = false)
                }
            }
            _state.value = newState
            return newState
        } finally {
            mutex.unlock()
        }
    }
}
