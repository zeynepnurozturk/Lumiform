package com.task.lumiform.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val SectionIndent = 12.dp

// How many sections this one is nested in; each SectionComponent sets it for its own content.
private val LocalSectionLevel = compositionLocalOf { 0 }

/**
 * A section: its title, then its [content] indented one step under the title.
 * A section placed inside another section's [content] is indented and gets a smaller title automatically.
 */
@Composable
fun SectionComponent(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val level = LocalSectionLevel.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TitleComponent(
            title = title,
            style = sectionTextStyle(level),
            modifier = Modifier.padding(top = 8.dp)
        )
        Column(
            modifier = Modifier.padding(start = SectionIndent),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompositionLocalProvider(LocalSectionLevel provides level + 1) {
                content()
            }
        }
    }
}

/**
 * Sections get a smaller font the deeper they are nested; [level] 0 is a section directly inside a page.
 */
@Composable
private fun sectionTextStyle(level: Int): TextStyle = when (level) {
    0 -> MaterialTheme.typography.titleLarge
    1 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
}
