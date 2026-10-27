package com.example.profiles.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.example.profiles.R

@Composable
fun GroupNameDialog(
    title: String,
    confirmText: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = ""
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val trimmed = text.trim()
    val duplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) && it != initial }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.group_name)) },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) ({ Text(stringResource(R.string.group_exists)) }) else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed); onDismiss() },
                enabled = trimmed.isNotEmpty() && !duplicate
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}