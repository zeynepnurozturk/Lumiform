package com.task.lumiform.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.task.lumiform.R

/**
 * The caller sets the size through [modifier]. The image is clickable only when [onClick] is given.
 * A plain box is drawn while the image loads, and a broken image icon if it can't be loaded (e.g. offline and not cached).
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
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        error = painterResource(R.drawable.ic_broken_image),
        fallback = painterResource(R.drawable.ic_broken_image),
        contentScale = ContentScale.Fit,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    )
}
