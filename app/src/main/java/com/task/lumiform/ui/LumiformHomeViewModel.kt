package com.task.lumiform.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.task.lumiform.R
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.mapper.toHomePages
import com.task.lumiform.ui.navigation.LumiformDestination
import com.task.lumiform.ui.navigation.LumiformNavigator
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType
import com.task.lumiform.ui.model.LumiformHomePageUiModel
import com.task.lumiform.ui.toast.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface LumiformHomeUiState {
    data object Loading : LumiformHomeUiState
    data object Success : LumiformHomeUiState
    data object Empty : LumiformHomeUiState
}

@HiltViewModel
class LumiformHomeViewModel @Inject constructor(
    private val lumiformTestUseCase: LumiformTestUseCase,
    private val navigator: LumiformNavigator,
    private val toastManager: ToastManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LumiformHomeUiState>(LumiformHomeUiState.Loading)
    val uiState: StateFlow<LumiformHomeUiState> = _uiState.asStateFlow()

    private val _lumiformTestResult: MutableStateFlow<List<LumiformHomePageUiModel>?> = MutableStateFlow(null)
    val lumiformTestResult = _lumiformTestResult.asStateFlow()

    // Selected response ids per choice question id. Restored from Room on load and saved on every tap.
    private val _selectedResponses = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
    val selectedResponses = _selectedResponses.asStateFlow()

    // Index of the page on screen. Kept in SavedStateHandle so it survives process death.
    val currentPageIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_CURRENT_PAGE_INDEX, 0)

    // Keeps saves in tap order, so the last tap is always the one left in Room.
    private val saveMutex = Mutex()

    val callbacks by lazy {
        LumiformHomeCallbacks(
            onResponseClick = ::onResponseClick,
            onPreviousClick = ::onPreviousClick,
            onNextClick = ::onNextClick,
            onFinishClick = ::onFinishClick,
            onImageClick = ::onImageClick,
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
                    _selectedResponses.value = lumiformTestUseCase.getAnswers()
                    _lumiformTestResult.value = pages
                    // A restored index can point past the end if the API now returns fewer pages.
                    savedStateHandle[KEY_CURRENT_PAGE_INDEX] =
                        currentPageIndex.value.coerceAtMost(pages.lastIndex).coerceAtLeast(0)
                    _uiState.value =
                        if (pages.isEmpty()) LumiformHomeUiState.Empty else LumiformHomeUiState.Success
                }
                // The screen stays as it is; the failure is only reported as a toast.
                is UseCaseResult.Error -> toastManager.showToast(result.error.toMessageRes())
                is UseCaseResult.Warning -> toastManager.showToast(R.string.error_server)
            }
        }
    }

    private fun UseCaseError.toMessageRes() =
        if (this is UseCaseError.NetworkError) R.string.error_network else R.string.error_server

    private fun onResponseClick(question: LumiformHomeItemUiModel.Choice, responseId: Int) {
        val pageId = currentPage()?.id ?: return
        val selections = _selectedResponses.updateAndGet { selections ->
            val selected = selections[question.id].orEmpty()
            val newSelected = when {
                question.selectionType == SelectionType.Single -> listOf(responseId)
                responseId in selected -> selected - responseId
                else -> selected + responseId
            }
            selections + (question.id to newSelected)
        }
        val selected = selections[question.id].orEmpty()
        viewModelScope.launch {
            saveMutex.withLock {
                lumiformTestUseCase.saveAnswer(pageId, question.id, selected)
            }
        }
    }

    private fun onPreviousClick() {
        val index = currentPageIndex.value
        if (index > 0) savedStateHandle[KEY_CURRENT_PAGE_INDEX] = index - 1
    }

    private fun onNextClick() {
        queueCurrentPageForSync()
        val index = currentPageIndex.value
        if (index < _lumiformTestResult.value.orEmpty().lastIndex) {
            savedStateHandle[KEY_CURRENT_PAGE_INDEX] = index + 1
        }
    }

    private fun onFinishClick() {
        queueCurrentPageForSync()
        navigator.navigate(LumiformDestination.Summary)
    }

    private fun onImageClick(src: String, title: String) {
        navigator.navigate(LumiformDestination.FullImage(src = src, title = title))
    }

    // Waits for pending saves so the worker finds this page's latest answers in Room.
    private fun queueCurrentPageForSync() {
        val pageId = currentPage()?.id ?: return
        viewModelScope.launch {
            saveMutex.withLock { lumiformTestUseCase.queuePageForSync(pageId) }
        }
    }

    private fun currentPage(): LumiformHomePageUiModel? =
        _lumiformTestResult.value?.getOrNull(currentPageIndex.value)

    private companion object {
        const val KEY_CURRENT_PAGE_INDEX = "current_page_index"
    }
}

data class LumiformHomeCallbacks(
    val onResponseClick: (question: LumiformHomeItemUiModel.Choice, responseId: Int) -> Unit,
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onFinishClick: () -> Unit,
    val onImageClick: (src: String, title: String) -> Unit,
)
