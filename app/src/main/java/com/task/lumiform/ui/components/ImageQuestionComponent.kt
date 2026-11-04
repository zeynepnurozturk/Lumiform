package com.task.lumiform.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An image question on a page: a [thumbnailSize] thumbnail with its title under it. Tapping either one calls [onClick].
 */
@Composable
fun ImageQuestionComponent(
    src: String,
    title: String,
    thumbnailSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(role = Role.Image, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ImageComponent(
            src = src,
            title = title,
            modifier = Modifier.size(thumbnailSize)
        )
        if (title.isNotEmpty()) {
            TextComponent(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}
