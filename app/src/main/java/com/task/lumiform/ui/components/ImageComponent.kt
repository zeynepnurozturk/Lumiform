package com.task.lumiform.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * The caller sets the size through [modifier]. The image is clickable only when [onClick] is given.
 */
@Composable
fun ImageComponent(
    src: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AsyncImage(
        model = src,
        contentDescription = title,
        contentScale = ContentScale.Fit,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    )
}
