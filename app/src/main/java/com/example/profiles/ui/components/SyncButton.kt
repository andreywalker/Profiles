package com.example.profiles.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.example.profiles.R
import com.example.profiles.sync.SyncState

/** Top-bar sync button: spins while syncing, then shows done / server down / error. */
@Composable
fun SyncButton(state: SyncState, onClick: () -> Unit) {
    val angle = if (state is SyncState.Syncing) {
        val transition = rememberInfiniteTransition(label = "sync")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing)),
            label = "syncAngle"
        )
        value
    } else 0f

    val icon = when (state) {
        SyncState.Idle, SyncState.Syncing -> Icons.Outlined.Sync
        is SyncState.Synced -> Icons.Outlined.CloudDone
        is SyncState.Error -> if (state.serverDown) Icons.Outlined.CloudOff else Icons.Outlined.SyncProblem
    }
    val tint = when (state) {
        is SyncState.Synced -> MaterialTheme.colorScheme.primary
        is SyncState.Error -> MaterialTheme.colorScheme.error
        else -> LocalContentColor.current
    }
    val description = stringResource(
        when (state) {
            SyncState.Idle -> R.string.sync
            SyncState.Syncing -> R.string.sync_in_progress
            is SyncState.Synced -> R.string.synced
            is SyncState.Error -> if (state.serverDown) R.string.sync_server_down else R.string.sync_error
        }
    )

    IconButton(onClick = onClick, enabled = state !is SyncState.Syncing) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.rotate(angle))
    }
}
