package com.task.lumiform.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.mapper.toSummaryPages
import com.task.lumiform.ui.navigation.LumiformNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LumiformSummaryUiState {
    data object Loading : LumiformSummaryUiState
    data object Success : LumiformSummaryUiState
    data class Error(val message: String) : LumiformSummaryUiState
}

@HiltViewModel
class LumiformSummaryViewModel @Inject constructor(
    private val lumiformTestUseCase: LumiformTestUseCase,
    private val navigator: LumiformNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LumiformSummaryUiState>(LumiformSummaryUiState.Loading)
    val uiState: StateFlow<LumiformSummaryUiState> = _uiState.asStateFlow()

    private val _summaryPages = MutableStateFlow<List<LumiformSummaryPageUiModel>>(emptyList())
    val summaryPages = _summaryPages.asStateFlow()

    private val _isSubmitted = MutableStateFlow(false)
    val isSubmitted = _isSubmitted.asStateFlow()

    private var pageIds: List<Int> = emptyList()

    val callbacks by lazy {
        LumiformSummaryCallbacks(
            onSubmitClick = ::onSubmitClick,
            onBackClick = navigator::back,
        )
    }

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            when (val result = lumiformTestUseCase()) {
                is UseCaseResult.Success -> {
                    pageIds = result.result.model.map { it.id }
                    val pages = result.result.model.toSummaryPages(lumiformTestUseCase.getAnswers())
                    _summaryPages.value = pages
                    _uiState.value = LumiformSummaryUiState.Success
                }
                is UseCaseResult.Error -> _uiState.value = LumiformSummaryUiState.Error(result.message)
                is UseCaseResult.Warning -> _uiState.value = LumiformSummaryUiState.Error(result.message)
            }
        }
    }

    // Answers are already in Room, so submitting only queues every page; pages already synced are skipped by the worker.
    // TODO: Once the submission is confirmed as successful, clear the back stack so the user can't return to the same QA,
    //  and delete the cached pages and answers from Room.
    private fun onSubmitClick() {
        if (_uiState.value != LumiformSummaryUiState.Success || _isSubmitted.value) return
        pageIds.forEach(lumiformTestUseCase::queuePageForSync)
        _isSubmitted.value = true
    }
}

data class LumiformSummaryCallbacks(
    val onSubmitClick: () -> Unit,
    val onBackClick: () -> Unit,
)
