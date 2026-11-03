package com.task.lumiform.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.mapper.toHomePages
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType
import com.task.lumiform.ui.model.LumiformHomePageUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LumiformHomeUiState {
    data object Loading : LumiformHomeUiState
    data object Success : LumiformHomeUiState
    data object Empty : LumiformHomeUiState
    data class Error(val message: String) : LumiformHomeUiState
}

@HiltViewModel
class LumiformHomeViewModel @Inject constructor(
    private val lumiformTestUseCase: LumiformTestUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LumiformHomeUiState>(LumiformHomeUiState.Loading)
    val uiState: StateFlow<LumiformHomeUiState> = _uiState.asStateFlow()

    private val _lumiformTestResult: MutableStateFlow<List<LumiformHomePageUiModel>?> = MutableStateFlow(null)
    val lumiformTestResult = _lumiformTestResult.asStateFlow()

    // Selected response ids per choice question id.
    // TODO: Maybe keep selections in Room instead of in memory, so they survive app restarts.
    private val _selectedResponses = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
    val selectedResponses = _selectedResponses.asStateFlow()

    // Index of the page on screen. Kept in SavedStateHandle so it survives process death.
    val currentPageIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_CURRENT_PAGE_INDEX, 0)

    val callbacks by lazy {
        LumiformHomeCallbacks(
            onResponseClick = ::onResponseClick,
            onPreviousClick = ::onPreviousClick,
            onNextClick = ::onNextClick,
            onFinishClick = ::onFinishClick,
        )
    }

    init {
        loadTestCase()
    }

    private fun loadTestCase() {
        _uiState.value = LumiformHomeUiState.Loading
        viewModelScope.launch {
            when (val result = lumiformTestUseCase()) {
                is UseCaseResult.Success -> {
                    val pages = result.result.model.toHomePages()
                    _lumiformTestResult.value = pages
                    // A restored index can point past the end if the API now returns fewer pages.
                    savedStateHandle[KEY_CURRENT_PAGE_INDEX] =
                        currentPageIndex.value.coerceAtMost(pages.lastIndex).coerceAtLeast(0)
                    _uiState.value =
                        if (pages.isEmpty()) LumiformHomeUiState.Empty else LumiformHomeUiState.Success
                }
                is UseCaseResult.Error -> _uiState.value = LumiformHomeUiState.Error(result.message)
                is UseCaseResult.Warning -> _uiState.value = LumiformHomeUiState.Error(result.message)
            }
        }
    }

    private fun onResponseClick(question: LumiformHomeItemUiModel.Choice, responseId: Int) {
        _selectedResponses.update { selections ->
            val selected = selections[question.id].orEmpty()
            val newSelected = when {
                question.selectionType == SelectionType.Single -> listOf(responseId)
                responseId in selected -> selected - responseId
                else -> selected + responseId
            }
            selections + (question.id to newSelected)
        }
    }

    private fun onPreviousClick() {
        val index = currentPageIndex.value
        if (index > 0) savedStateHandle[KEY_CURRENT_PAGE_INDEX] = index - 1
    }

    private fun onNextClick() {
        val index = currentPageIndex.value
        if (index < _lumiformTestResult.value.orEmpty().lastIndex) {
            savedStateHandle[KEY_CURRENT_PAGE_INDEX] = index + 1
        }
    }

    private fun onFinishClick() {
        // TODO: Show a summary page with the selected answers and an option to go back and edit them.
        // TODO: Submit selectedResponses once there is an endpoint for it.
    }

    private companion object {
        const val KEY_CURRENT_PAGE_INDEX = "current_page_index"
    }
}

data class LumiformHomeCallbacks(
    val onResponseClick: (question: LumiformHomeItemUiModel.Choice, responseId: Int) -> Unit,
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onFinishClick: () -> Unit,
)
