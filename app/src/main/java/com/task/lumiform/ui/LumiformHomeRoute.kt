package com.task.lumiform.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.task.lumiform.ui.components.ChoiceComponent
import com.task.lumiform.ui.components.ImageComponent
import com.task.lumiform.ui.components.SectionComponent
import com.task.lumiform.ui.components.TextComponent
import com.task.lumiform.ui.components.TitleComponent
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomePageUiModel

private val ThumbnailSize = 120.dp

@Composable
fun LumiformHomeRoute(
    onImageClick: (src: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LumiformHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lumiformTestResult by viewModel.lumiformTestResult.collectAsStateWithLifecycle()
    val selectedResponses by viewModel.selectedResponses.collectAsStateWithLifecycle()

    LumiformHomeScreen(
        modifier = modifier,
        uiState = uiState,
        lumiformTestResult = lumiformTestResult,
        selectedResponses = selectedResponses,
        callbacks = viewModel.callbacks,
        onImageClick = onImageClick,
    )
}

@Composable
fun LumiformHomeScreen(
    uiState: LumiformHomeUiState,
    lumiformTestResult: List<LumiformHomePageUiModel>?,
    selectedResponses: Map<Int, List<Int>>,
    callbacks: LumiformHomeCallbacks,
    onImageClick: (src: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                LumiformHomeUiState.Loading -> CircularProgressIndicator()
                is LumiformHomeUiState.Error -> TextComponent(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                LumiformHomeUiState.Success -> LumiformHomeContent(
                    pages = lumiformTestResult.orEmpty(),
                    selectedResponses = selectedResponses,
                    callbacks = callbacks,
                    onImageClick = onImageClick,
                )
            }
        }
    }
}

@Composable
private fun LumiformHomeContent(
    pages: List<LumiformHomePageUiModel>,
    selectedResponses: Map<Int, List<Int>>,
    callbacks: LumiformHomeCallbacks,
    onImageClick: (src: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pages.forEach { page ->
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
                    onImageClick = onImageClick,
                )
            }
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
    onImageClick: (src: String, title: String) -> Unit,
) {
    when (item) {
        is LumiformHomeItemUiModel.Section -> SectionComponent(title = item.title) {
            item.items.forEach { child ->
                LumiformHomeItem(
                    item = child,
                    selectedResponses = selectedResponses,
                    callbacks = callbacks,
                    onImageClick = onImageClick,
                )
            }
        }
        is LumiformHomeItemUiModel.Text -> TextComponent(
            text = item.content,
            style = MaterialTheme.typography.bodySmall
        )
        is LumiformHomeItemUiModel.Image -> ImageComponent(
            src = item.src,
            title = item.title,
            modifier = Modifier.size(ThumbnailSize),
            onClick = { onImageClick(item.src, item.title) }
        )
        is LumiformHomeItemUiModel.Choice -> ChoiceComponent(
            question = item,
            selected = selectedResponses[item.id].orEmpty(),
            onResponseClick = { responseId -> callbacks.onResponseClick(item, responseId) }
        )
    }
}
