package com.example.profiles.ui.components


import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.profiles.R
import com.example.profiles.data.FieldDef
import com.example.profiles.data.Group


@Composable
fun Group.displayName(): String =
    if (isDefault) stringResource(R.string.no_group) else name

@Composable
fun FieldDef.displayName(): String = when (builtInKey) {
    "favorite_color" -> stringResource(R.string.favorite_color)
    "wishlist" -> stringResource(R.string.wishlist)
    "facts" -> stringResource(R.string.facts)
    else -> name
}