package com.task.lumiform.ui

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
    data class Error(val message: String) : LumiformHomeUiState
}

@HiltViewModel
class LumiformHomeViewModel @Inject constructor(
    private val lumiformTestUseCase: LumiformTestUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LumiformHomeUiState>(LumiformHomeUiState.Loading)
    val uiState: StateFlow<LumiformHomeUiState> = _uiState.asStateFlow()

    private val _lumiformTestResult: MutableStateFlow<List<LumiformHomePageUiModel>?> = MutableStateFlow(null)
    val lumiformTestResult = _lumiformTestResult.asStateFlow()

    // Selected response ids per choice question id.
    // TODO: Maybe keep selections in Room instead of in memory, so they survive app restarts.
    private val _selectedResponses = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
    val selectedResponses = _selectedResponses.asStateFlow()

    val callbacks by lazy {
        LumiformHomeCallbacks(
            onResponseClick = ::onResponseClick,
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
                    _lumiformTestResult.value = result.result.model.toHomePages()
                    _uiState.value = LumiformHomeUiState.Success
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
}

data class LumiformHomeCallbacks(
    val onResponseClick: (question: LumiformHomeItemUiModel.Choice, responseId: Int) -> Unit,
)
