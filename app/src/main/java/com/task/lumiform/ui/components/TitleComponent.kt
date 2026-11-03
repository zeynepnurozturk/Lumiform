package com.task.lumiform.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Title of a page, section or screen, built on [TextComponent]. The caller picks the [style] for its level.
 */
@Composable
fun TitleComponent(
    title: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    TextComponent(
        text = title,
        style = style,
        modifier = modifier
    )
}
