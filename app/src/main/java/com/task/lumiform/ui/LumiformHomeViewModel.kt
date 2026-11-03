package com.task.lumiform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _lumiformTestResult: MutableStateFlow<List<PageDomainModel>?> = MutableStateFlow(null)
    val lumiformTestResult = _lumiformTestResult.asStateFlow()

    val callbacks by lazy {
        LumiformHomeCallbacks()
    }

    init {
        loadTestCase()
    }

    private fun loadTestCase() {
        _uiState.value = LumiformHomeUiState.Loading
        viewModelScope.launch {
            when (val result = lumiformTestUseCase()) {
                is UseCaseResult.Success -> {
                    _lumiformTestResult.value = result.result.model
                    _uiState.value = LumiformHomeUiState.Success
                }
                is UseCaseResult.Error -> _uiState.value = LumiformHomeUiState.Error(result.message)
                is UseCaseResult.Warning -> _uiState.value = LumiformHomeUiState.Error(result.message)
            }
        }
    }
}


class LumiformHomeCallbacks
