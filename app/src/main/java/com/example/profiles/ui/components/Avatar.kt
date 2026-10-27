package com.example.profiles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun Avatar(photoPath: String?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val shape = Modifier.size(size).clip(CircleShape)
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.then(shape)
        )
    } else {
        Box(
            modifier = modifier.then(shape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}