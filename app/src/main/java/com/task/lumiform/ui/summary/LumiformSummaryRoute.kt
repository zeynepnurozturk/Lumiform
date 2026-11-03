package com.task.lumiform.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.task.lumiform.R
import com.task.lumiform.ui.components.SectionComponent
import com.task.lumiform.ui.components.TextComponent
import com.task.lumiform.ui.components.TitleComponent

@Composable
fun LumiformSummaryRoute(
    modifier: Modifier = Modifier,
    viewModel: LumiformSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaryPages by viewModel.summaryPages.collectAsStateWithLifecycle()
    val isSubmitted by viewModel.isSubmitted.collectAsStateWithLifecycle()

    LumiformSummaryScreen(
        modifier = modifier,
        uiState = uiState,
        summaryPages = summaryPages,
        isSubmitted = isSubmitted,
        callbacks = viewModel.callbacks,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiformSummaryScreen(
    uiState: LumiformSummaryUiState,
    summaryPages: List<LumiformSummaryPageUiModel>,
    isSubmitted: Boolean,
    callbacks: LumiformSummaryCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    TitleComponent(
                        title = stringResource(R.string.summary_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = callbacks.onBackClick) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState == LumiformSummaryUiState.Success) {
                BottomAppBar {
                    Button(
                        onClick = callbacks.onSubmitClick,
                        enabled = !isSubmitted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextComponent(
                            text = stringResource(
                                if (isSubmitted) R.string.summary_submitted else R.string.summary_submit
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
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
                LumiformSummaryUiState.Loading -> CircularProgressIndicator()
                is LumiformSummaryUiState.Error -> TextComponent(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                LumiformSummaryUiState.Success -> SummaryContent(pages = summaryPages)
            }
        }
    }
}

@Composable
private fun SummaryContent(
    pages: List<LumiformSummaryPageUiModel>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pages, key = { it.id }) { page ->
            SectionComponent(title = page.title) {
                page.answers.forEach { answer -> SummaryAnswer(answer = answer) }
            }
        }
    }
}

@Composable
private fun SummaryAnswer(answer: LumiformSummaryAnswerUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextComponent(text = answer.question, style = MaterialTheme.typography.bodyMedium)
        TextComponent(
            text = answer.answers.joinToString().ifEmpty { stringResource(R.string.summary_no_answer) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
