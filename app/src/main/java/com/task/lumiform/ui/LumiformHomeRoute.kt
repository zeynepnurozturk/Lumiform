package com.task.lumiform.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.task.lumiform.domain.model.LumiformItemDomainModel

@Composable
fun LumiformHomeRoute(
    modifier: Modifier = Modifier,
    viewModel: LumiformHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lumiformTestResult by viewModel.lumiformTestResult.collectAsStateWithLifecycle()

    LumiformHomeScreen(
        modifier = modifier,
        uiState = uiState,
        lumiformTestResult = lumiformTestResult,
        callbacks = viewModel.callbacks,
    )
}

@Suppress("UnusedParameter")
@Composable
fun LumiformHomeScreen(
    uiState: LumiformHomeUiState,
    lumiformTestResult: List<LumiformItemDomainModel>?,
    callbacks: LumiformHomeCallbacks,
    modifier: Modifier = Modifier,
) {
}
