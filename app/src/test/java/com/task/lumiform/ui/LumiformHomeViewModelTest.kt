package com.task.lumiform.ui

import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LumiformHomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in Loading until the use case returns`() = runTest {
        val repository = FakeRepository()

        val viewModel = LumiformHomeViewModel(LumiformTestUseCase(repository))

        assertEquals(LumiformHomeUiState.Loading, viewModel.uiState.value)
        assertNull(viewModel.lumiformTestResult.value)
    }

    @Test
    fun `success exposes pages and Success state`() = runTest {
        val repository = FakeRepository()
        val viewModel = LumiformHomeViewModel(LumiformTestUseCase(repository))
        val pages = listOf(PageDomainModel(id = 1, title = "Main Page"))

        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))

        assertEquals(LumiformHomeUiState.Success, viewModel.uiState.value)
        assertEquals(pages, viewModel.lumiformTestResult.value)
    }

    @Test
    fun `error exposes Error state with message`() = runTest {
        val repository = FakeRepository()
        val viewModel = LumiformHomeViewModel(LumiformTestUseCase(repository))

        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("boom")))

        assertEquals(LumiformHomeUiState.Error("boom"), viewModel.uiState.value)
        assertNull(viewModel.lumiformTestResult.value)
    }

    private class FakeRepository : LumiformTestRepository {
        val result = CompletableDeferred<UseCaseResult<LumiformTestUseCase.Result>>()
        override suspend fun getTestCase() = result.await()
    }
}
