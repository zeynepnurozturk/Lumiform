package com.task.lumiform.ui

import androidx.lifecycle.SavedStateHandle
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.model.TextQuestionDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomePageUiModel
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

        val viewModel = newViewModel(repository)

        assertEquals(LumiformHomeUiState.Loading, viewModel.uiState.value)
        assertNull(viewModel.lumiformTestResult.value)
    }

    @Test
    fun `success exposes pages and Success state`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)
        val pages = listOf(PageDomainModel(id = 1, title = "Main Page"))

        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))

        assertEquals(LumiformHomeUiState.Success, viewModel.uiState.value)
        assertEquals(
            listOf(LumiformHomePageUiModel(id = 1, title = "Main Page", items = emptyList())),
            viewModel.lumiformTestResult.value
        )
    }

    @Test
    fun `single selection keeps only the last tapped response`() = runTest {
        val viewModel = loadedViewModel(multipleSelection = false)
        val question = viewModel.choiceQuestion()

        viewModel.callbacks.onResponseClick(question, 1)
        viewModel.callbacks.onResponseClick(question, 2)
        viewModel.callbacks.onResponseClick(question, 2)

        assertEquals(mapOf(QUESTION_ID to listOf(2)), viewModel.selectedResponses.value)
    }

    @Test
    fun `multiple selection toggles each response`() = runTest {
        val viewModel = loadedViewModel(multipleSelection = true)
        val question = viewModel.choiceQuestion()

        viewModel.callbacks.onResponseClick(question, 1)
        viewModel.callbacks.onResponseClick(question, 2)
        viewModel.callbacks.onResponseClick(question, 1)

        assertEquals(mapOf(QUESTION_ID to listOf(2)), viewModel.selectedResponses.value)
    }

    @Test
    fun `multiple selection keeps tap order`() = runTest {
        val viewModel = loadedViewModel(multipleSelection = true)
        val question = viewModel.choiceQuestion()

        viewModel.callbacks.onResponseClick(question, 2)
        viewModel.callbacks.onResponseClick(question, 1)

        assertEquals(mapOf(QUESTION_ID to listOf(2, 1)), viewModel.selectedResponses.value)
    }

    @Test
    fun `error exposes Error state with message`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("boom")))

        assertEquals(LumiformHomeUiState.Error("boom"), viewModel.uiState.value)
        assertNull(viewModel.lumiformTestResult.value)
    }

    @Test
    fun `starts on the first page`() = runTest {
        val viewModel = pagedViewModel(pageCount = 3)

        assertEquals(0, viewModel.currentPageIndex.value)
    }

    @Test
    fun `next moves forward and stops on the last page`() = runTest {
        val viewModel = pagedViewModel(pageCount = 3)

        viewModel.callbacks.onNextClick()
        assertEquals(1, viewModel.currentPageIndex.value)
        viewModel.callbacks.onNextClick()
        viewModel.callbacks.onNextClick()

        assertEquals(2, viewModel.currentPageIndex.value)
    }

    @Test
    fun `previous moves back and stops on the first page`() = runTest {
        val viewModel = pagedViewModel(pageCount = 3)
        viewModel.callbacks.onNextClick()
        viewModel.callbacks.onNextClick()

        viewModel.callbacks.onPreviousClick()
        assertEquals(1, viewModel.currentPageIndex.value)
        viewModel.callbacks.onPreviousClick()
        viewModel.callbacks.onPreviousClick()

        assertEquals(0, viewModel.currentPageIndex.value)
    }

    @Test
    fun `selections are kept when moving between pages`() = runTest {
        val viewModel = loadedViewModel(multipleSelection = false, extraPages = 1)
        val question = viewModel.choiceQuestion()
        viewModel.callbacks.onResponseClick(question, 1)

        viewModel.callbacks.onNextClick()
        viewModel.callbacks.onPreviousClick()

        assertEquals(mapOf(QUESTION_ID to listOf(1)), viewModel.selectedResponses.value)
    }

    @Test
    fun `restores the saved page index`() = runTest {
        val viewModel = pagedViewModel(pageCount = 3, savedStateHandle = savedPageIndex(2))

        assertEquals(2, viewModel.currentPageIndex.value)
    }

    @Test
    fun `saved page index past the last page is moved to the last page`() = runTest {
        val viewModel = pagedViewModel(pageCount = 2, savedStateHandle = savedPageIndex(5))

        assertEquals(1, viewModel.currentPageIndex.value)
    }

    @Test
    fun `no pages gives Empty state and next does nothing`() = runTest {
        val viewModel = pagedViewModel(pageCount = 0)

        viewModel.callbacks.onNextClick()

        assertEquals(LumiformHomeUiState.Empty, viewModel.uiState.value)
        assertEquals(emptyList<LumiformHomePageUiModel>(), viewModel.lumiformTestResult.value)
        assertEquals(0, viewModel.currentPageIndex.value)
    }

    private fun newViewModel(
        repository: FakeRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = LumiformHomeViewModel(LumiformTestUseCase(repository), savedStateHandle)

    private fun pagedViewModel(
        pageCount: Int,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): LumiformHomeViewModel {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository, savedStateHandle)
        val pages = (1..pageCount).map { id ->
            PageDomainModel(id = id, title = "Page $id", items = listOf(TextQuestionDomainModel(id = id * 100)))
        }
        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))
        return viewModel
    }

    private fun savedPageIndex(index: Int) = SavedStateHandle(mapOf("current_page_index" to index))

    private fun loadedViewModel(multipleSelection: Boolean, extraPages: Int = 0): LumiformHomeViewModel {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)
        val choice = ChoiceQuestionDomainModel(
            id = QUESTION_ID,
            content = "Question",
            responseSet = ResponseSetDomainModel(
                id = 100,
                multipleSelection = multipleSelection,
                responses = listOf(
                    ResponseOptionDomainModel(id = 1, label = "A"),
                    ResponseOptionDomainModel(id = 2, label = "B")
                )
            )
        )
        val pages = listOf(PageDomainModel(id = 1, title = "Page", items = listOf(choice))) +
            (1..extraPages).map { PageDomainModel(id = 1 + it, title = "Page ${1 + it}") }
        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))
        return viewModel
    }

    private fun LumiformHomeViewModel.choiceQuestion() =
        lumiformTestResult.value!!.first().items.filterIsInstance<LumiformHomeItemUiModel.Choice>().single()

    private companion object {
        const val QUESTION_ID = 10
    }

    private class FakeRepository : LumiformTestRepository {
        val result = CompletableDeferred<UseCaseResult<LumiformTestUseCase.Result>>()
        override suspend fun getTestCase() = result.await()
    }
}
