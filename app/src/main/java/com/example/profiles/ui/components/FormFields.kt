package com.example.profiles.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.profiles.R

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    showError: Boolean = false,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val isError = required && showError && value.isBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.required)) }) else null,
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** A read-only field that opens something on tap (date picker, dropdown). */
@Composable
fun ClickableField(
    value: String,
    label: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector,
    isError: Boolean = false
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            trailingIcon = { Icon(trailingIcon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // transparent layer on top catches the tap
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}