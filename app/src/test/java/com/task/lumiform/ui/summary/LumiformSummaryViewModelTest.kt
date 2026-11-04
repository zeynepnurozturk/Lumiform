package com.task.lumiform.ui.summary

import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.PageDomainModel
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.domain.model.ResponseSetDomainModel
import com.task.lumiform.domain.repository.LumiformTestRepository
import com.task.lumiform.domain.usecase.LumiformTestUseCase
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import com.task.lumiform.ui.navigation.LumiformNavigator
import com.task.lumiform.ui.navigation.NavigationCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LumiformSummaryViewModelTest {

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
        val viewModel = newViewModel(FakeRepository())

        assertEquals(LumiformSummaryUiState.Loading, viewModel.uiState.value)
        assertEquals(emptyList<LumiformSummaryPageUiModel>(), viewModel.summaryPages.value)
    }

    @Test
    fun `success shows the saved answers per page`() = runTest {
        val repository = FakeRepository().apply { savedAnswers[QUESTION_ID] = listOf(2) }
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Success(LumiformTestUseCase.Result(listOf(choicePage(id = 1)))))

        assertEquals(LumiformSummaryUiState.Success, viewModel.uiState.value)
        assertEquals(
            listOf(
                LumiformSummaryPageUiModel(
                    id = 1,
                    title = "Page 1",
                    answers = listOf(LumiformSummaryAnswerUiModel(QUESTION_ID, "Question", listOf("B")))
                )
            ),
            viewModel.summaryPages.value
        )
    }

    @Test
    fun `error shows its message`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Error(UseCaseError.NetworkError("No connection")))

        assertEquals(LumiformSummaryUiState.Error("No connection"), viewModel.uiState.value)
    }

    @Test
    fun `warning shows its message`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        repository.result.complete(UseCaseResult.Warning("Empty"))

        assertEquals(LumiformSummaryUiState.Error("Empty"), viewModel.uiState.value)
    }

    @Test
    fun `submit queues every page once and marks the summary submitted`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)
        repository.result.complete(
            UseCaseResult.Success(LumiformTestUseCase.Result(listOf(choicePage(id = 1), choicePage(id = 2))))
        )

        viewModel.callbacks.onSubmitClick()
        viewModel.callbacks.onSubmitClick()

        assertEquals(listOf(1, 2), repository.queuedPageIds)
        assertTrue(viewModel.isSubmitted.value)
    }

    @Test
    fun `submit does nothing before the summary is loaded`() = runTest {
        val repository = FakeRepository()
        val viewModel = newViewModel(repository)

        viewModel.callbacks.onSubmitClick()

        assertEquals(emptyList<Int>(), repository.queuedPageIds)
        assertFalse(viewModel.isSubmitted.value)
    }

    @Test
    fun `back asks the navigator to go back`() = runTest {
        val navigator = LumiformNavigator()
        val viewModel = newViewModel(FakeRepository(), navigator)

        viewModel.callbacks.onBackClick()

        assertEquals(NavigationCommand.Back, navigator.commands.first())
    }

    private fun newViewModel(repository: FakeRepository, navigator: LumiformNavigator = LumiformNavigator()) =
        LumiformSummaryViewModel(LumiformTestUseCase(repository), navigator)

    private fun choicePage(id: Int) = PageDomainModel(
        id = id,
        title = "Page $id",
        items = listOf(
            ChoiceQuestionDomainModel(
                id = if (id == 1) QUESTION_ID else QUESTION_ID + id,
                content = "Question",
                responseSet = ResponseSetDomainModel(
                    id = 100 + id,
                    responses = listOf(
                        ResponseOptionDomainModel(id = 1, label = "A"),
                        ResponseOptionDomainModel(id = 2, label = "B")
                    )
                )
            )
        )
    )

    private companion object {
        const val QUESTION_ID = 10
    }

    private class FakeRepository : LumiformTestRepository {
        val result = CompletableDeferred<UseCaseResult<LumiformTestUseCase.Result>>()
        val savedAnswers = mutableMapOf<Int, List<Int>>()
        val queuedPageIds = mutableListOf<Int>()
        override suspend fun getTestCase() = result.await()
        override suspend fun getAnswers() = savedAnswers.toMap()
        override suspend fun saveAnswer(pageId: Int, questionId: Int, selectedResponseIds: List<Int>) = Unit
        override fun queuePageForSync(pageId: Int) {
            queuedPageIds += pageId
        }
        override suspend fun syncPageAnswers(pageId: Int) = UseCaseResult.Success(Unit)

        override suspend fun submitAll() = UseCaseResult.Success(Unit)
    }
}
