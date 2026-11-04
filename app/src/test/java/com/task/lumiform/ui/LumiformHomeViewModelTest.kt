package com.task.lumiform.ui

import androidx.lifecycle.SavedStateHandle
import com.task.lumiform.R
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.model.SyncStatus
import com.task.lumiform.domain.model.TextQuestionDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.repository.SyncStatusRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomePageUiModel
import com.task.lumiform.ui.navigation.LumiformNavigator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
        assertNull(viewModel.pages.value)
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
            viewModel.pages.value
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
    fun `network error with nothing cached shows the network error state`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("boom")))

        assertEquals(LumiformHomeUiState.Error(R.string.error_network), viewModel.uiState.value)
        assertNull(viewModel.pages.value)
    }

    @Test
    fun `any other error shows the server error state`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Error(UseCaseError.ApiError(code = 500, message = "Server error")))

        assertEquals(LumiformHomeUiState.Error(R.string.error_server), viewModel.uiState.value)
    }

    @Test
    fun `warning shows the server error state`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Warning("odd response"))

        assertEquals(LumiformHomeUiState.Error(R.string.error_server), viewModel.uiState.value)
    }

    @Test
    fun `retry shows Loading and then the pages when the next load succeeds`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)
        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("boom")))
        repository.result = CompletableDeferred()

        viewModel.callbacks.onRetryClick()
        assertEquals(LumiformHomeUiState.Loading, viewModel.uiState.value)
        val pages = listOf(PageDomainModel(id = 1, title = "Main Page"))
        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))

        assertEquals(LumiformHomeUiState.Success, viewModel.uiState.value)
        assertEquals(1, viewModel.pages.value?.size)
    }

    @Test
    fun `retry that fails again stays on the error state`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)
        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("boom")))
        repository.result = CompletableDeferred()

        viewModel.callbacks.onRetryClick()
        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("still offline")))

        assertEquals(LumiformHomeUiState.Error(R.string.error_network), viewModel.uiState.value)
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
        assertEquals(emptyList<LumiformHomePageUiModel>(), viewModel.pages.value)
        assertEquals(0, viewModel.currentPageIndex.value)
    }

    @Test
    fun `tapping a response saves it with its page`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(multipleSelection = true, repository = repository)
        val question = viewModel.choiceQuestion()

        viewModel.callbacks.onResponseClick(question, 1)
        viewModel.callbacks.onResponseClick(question, 2)

        assertEquals(mapOf(QUESTION_ID to listOf(1, 2)), repository.savedAnswers)
        assertEquals(1, repository.savedPageIds[QUESTION_ID])
    }

    @Test
    fun `saved answers are restored on load`() = runTest {
        val repository = FakeRepository().apply { savedAnswers[QUESTION_ID] = listOf(2) }

        val viewModel = loadedViewModel(multipleSelection = false, repository = repository)

        assertEquals(mapOf(QUESTION_ID to listOf(2)), viewModel.selectedResponses.value)
    }

    @Test
    fun `next and finish queue the page being left`() = runTest {
        val repository = FakeRepository()
        val viewModel = pagedViewModel(pageCount = 2, repository = repository)

        viewModel.callbacks.onNextClick()
        viewModel.callbacks.onFinishClick()

        assertEquals(listOf(1, 2), repository.queuedPageIds)
    }

    @Test
    fun `previous does not queue the page`() = runTest {
        val repository = FakeRepository()
        val viewModel = pagedViewModel(pageCount = 2, savedStateHandle = savedPageIndex(1), repository = repository)

        viewModel.callbacks.onPreviousClick()

        assertEquals(emptyList<Int>(), repository.queuedPageIds)
    }

    @Test
    fun `sync status follows the repository while collected`() = runTest {
        val syncStatus = FakeSyncStatusRepository()
        val viewModel = newViewModel(FakeRepository(), syncStatusRepository = syncStatus)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.syncStatus.collect {} }

        assertEquals(SyncStatus.Synced, viewModel.syncStatus.value)
        syncStatus.status.value = SyncStatus.WaitingForNetwork
        assertEquals(SyncStatus.WaitingForNetwork, viewModel.syncStatus.value)
        syncStatus.status.value = SyncStatus.Sending
        assertEquals(SyncStatus.Sending, viewModel.syncStatus.value)

        collector.cancel()
    }

    private fun newViewModel(
        repository: FakeRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        syncStatusRepository: SyncStatusRepository = FakeSyncStatusRepository(),
    ) = LumiformHomeViewModel(
        LumiformTestUseCase(repository),
        LumiformNavigator(),
        savedStateHandle,
        syncStatusRepository,
    )

    private class FakeSyncStatusRepository : SyncStatusRepository {
        val status = MutableStateFlow(SyncStatus.Synced)
        override fun observeSyncStatus() = status
    }

    private fun pagedViewModel(
        pageCount: Int,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        repository: FakeRepository = FakeRepository(),
    ): LumiformHomeViewModel {
        val viewModel = newViewModel(repository, savedStateHandle)
        val pages = (1..pageCount).map { id ->
            PageDomainModel(id = id, title = "Page $id", items = listOf(TextQuestionDomainModel(id = id * 100)))
        }
        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(pages)))
        return viewModel
    }

    private fun savedPageIndex(index: Int) = SavedStateHandle(mapOf("current_page_index" to index))

    private fun loadedViewModel(
        multipleSelection: Boolean,
        extraPages: Int = 0,
        repository: FakeRepository = FakeRepository(),
    ): LumiformHomeViewModel {
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
        pages.value!!.first().items.filterIsInstance<LumiformHomeItemUiModel.Choice>().single()

    private companion object {
        const val QUESTION_ID = 10
    }

    private class FakeRepository : LumiformTestRepository {
        // Replace with a new deferred before a retry to give the next load its own result.
        var result = CompletableDeferred<UseCaseResult<LumiformTestUseCase.Result>>()
        val savedAnswers = mutableMapOf<Int, List<Int>>()
        val savedPageIds = mutableMapOf<Int, Int>()
        val queuedPageIds = mutableListOf<Int>()
        override suspend fun getTestCase() = result.await()
        override suspend fun getAnswers() = savedAnswers.toMap()
        override suspend fun saveAnswer(
            pageId: Int,
            questionId: Int,
            selectedResponseIds: List<Int>
        ) {
            savedAnswers[questionId] = selectedResponseIds
            savedPageIds[questionId] = pageId
        }
        override fun queuePageForSync(pageId: Int) {
            queuedPageIds += pageId
        }
        override suspend fun syncPageAnswers(pageId: Int) = UseCaseResult.Success(Unit)

        override suspend fun submitAll() = UseCaseResult.Success(Unit)
    }
}
