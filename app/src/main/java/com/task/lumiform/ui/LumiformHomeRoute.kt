package com.task.lumiform.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.task.lumiform.R
import com.task.lumiform.domain.model.SyncStatus
import com.task.lumiform.ui.components.ChoiceComponent
import com.task.lumiform.ui.components.ImageQuestionComponent
import com.task.lumiform.ui.components.PageNavigationComponent
import com.task.lumiform.ui.components.SectionComponent
import com.task.lumiform.ui.components.SyncStatusComponent
import com.task.lumiform.ui.components.TextComponent
import com.task.lumiform.ui.components.TitleComponent
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomePageUiModel

private val ThumbnailSize = 120.dp

@Composable
fun LumiformHomeRoute(
    modifier: Modifier = Modifier,
    viewModel: LumiformHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val selectedResponses by viewModel.selectedResponses.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    LumiformHomeScreen(
        modifier = modifier,
        uiState = uiState,
        pages = pages,
        currentPageIndex = currentPageIndex,
        selectedResponses = selectedResponses,
        callbacks = viewModel.callbacks,
        syncStatus = syncStatus,
    )
}

@Composable
fun LumiformHomeScreen(
    uiState: LumiformHomeUiState,
    pages: List<LumiformHomePageUiModel>?,
    currentPageIndex: Int,
    selectedResponses: Map<Int, List<Int>>,
    callbacks: LumiformHomeCallbacks,
    modifier: Modifier = Modifier,
    syncStatus: SyncStatus = SyncStatus.Synced,
) {
    val loadedPages = pages.orEmpty()
    val showPage = uiState == LumiformHomeUiState.Success

    // System back goes to the previous page; only the first page leaves the screen.
    BackHandler(enabled = showPage && currentPageIndex > 0) {
        callbacks.onPreviousClick()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showPage) SyncStatusComponent(status = syncStatus)
        },
        bottomBar = {
            if (showPage) {
                PageNavigationComponent(
                    pageIndex = currentPageIndex,
                    pageCount = loadedPages.size,
                    onPreviousClick = callbacks.onPreviousClick,
                    onNextClick = callbacks.onNextClick,
                    onFinishClick = callbacks.onFinishClick,
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                LumiformHomeUiState.Loading -> CircularProgressIndicator()
                LumiformHomeUiState.Empty -> EmptyPageContent(message = stringResource(R.string.pages_empty))
                is LumiformHomeUiState.Error -> ErrorContent(
                    message = stringResource(uiState.messageRes),
                    onRetryClick = callbacks.onRetryClick,
                )
                LumiformHomeUiState.Success -> {
                    val currentPage = loadedPages[currentPageIndex]
                    key(currentPage.id) {
                        LumiformPageContent(
                            page = currentPage,
                            selectedResponses = selectedResponses,
                            callbacks = callbacks,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LumiformPageContent(
    page: LumiformHomePageUiModel,
    selectedResponses: Map<Int, List<Int>>,
    callbacks: LumiformHomeCallbacks,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = page.id) {
            TitleComponent(
                title = page.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        items(page.items, key = { it.id }) { item ->
            LumiformHomeItem(
                item = item,
                selectedResponses = selectedResponses,
                callbacks = callbacks,
            )
        }
    }
}

/**
 * Message shown when there is nothing to draw, either no pages at all or a page without items.
 */
@Composable
private fun EmptyPageContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        TextComponent(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Shown when the first load fails and nothing is cached yet. Retry runs the load again.
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmptyPageContent(message = message)
        Button(onClick = onRetryClick) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

/**
 * Draws one item. A section draws its own items the same way, so any nesting depth works.
 */
@Composable
private fun LumiformHomeItem(
    item: LumiformHomeItemUiModel,
    selectedResponses: Map<Int, List<Int>>,
    callbacks: LumiformHomeCallbacks,
) {
    when (item) {
        is LumiformHomeItemUiModel.Section -> SectionComponent(title = item.title) {
            item.items.forEach { child ->
                LumiformHomeItem(
                    item = child,
                    selectedResponses = selectedResponses,
                    callbacks = callbacks,
                )
            }
        }
        is LumiformHomeItemUiModel.Text -> TextComponent(
            text = item.content,
            style = MaterialTheme.typography.bodySmall
        )
        is LumiformHomeItemUiModel.Image -> ImageQuestionComponent(
            src = item.src,
            title = item.title,
            thumbnailSize = ThumbnailSize,
            onClick = { callbacks.onImageClick(item.src, item.title) }
        )
        is LumiformHomeItemUiModel.Choice -> ChoiceComponent(
            question = item,
            selected = selectedResponses[item.id].orEmpty(),
            onResponseClick = { responseId -> callbacks.onResponseClick(item, responseId) }
        )
    }
}
