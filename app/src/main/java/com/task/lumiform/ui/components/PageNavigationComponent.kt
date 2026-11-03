package com.task.lumiform.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.task.lumiform.R

/**
 * Bottom bar to move between pages. Previous is disabled on the first page and Next turns into Finish on the last one.
 */
@Composable
fun PageNavigationComponent(
    pageIndex: Int,
    pageCount: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastPage = pageIndex == pageCount - 1
    BottomAppBar(modifier = modifier) {
        OutlinedButton(onClick = onPreviousClick, enabled = pageIndex > 0) {
            TextComponent(
                text = stringResource(R.string.page_previous),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TextComponent(
                text = stringResource(R.string.page_indicator, pageIndex + 1, pageCount),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Button(onClick = if (isLastPage) onFinishClick else onNextClick) {
            TextComponent(
                text = stringResource(if (isLastPage) R.string.page_finish else R.string.page_next),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
