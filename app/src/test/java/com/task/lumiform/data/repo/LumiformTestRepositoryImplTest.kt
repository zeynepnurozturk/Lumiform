package com.task.lumiform.data.repo

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.ChoiceQuestionDto
import com.task.lumiform.data.remote.dto.PageDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.dto.SectionDto
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.domain.model.ChoiceQuestionDomainModel
import com.task.lumiform.domain.model.SectionDomainModel
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LumiformTestRepositoryImplTest {

    @Test
    fun `success maps dto tree to domain model`() = runTest {
        val dto = PageDto(
            id = 1,
            title = null,
            items = listOf(
                SectionDto(
                    id = 2,
                    title = "Section",
                    items = listOf(
                        ChoiceQuestionDto(
                            id = 3,
                            content = "Question",
                            responseSet = ResponseSetDto(
                                id = 4,
                                multipleSelection = true,
                                responses = listOf(ResponseOptionDto(id = 5, label = "A", score = null))
                            )
                        )
                    )
                )
            )
        )
        val repository = LumiformTestRepositoryImpl(FakeService(NetworkResult.Success(listOf(dto))))

        val result = repository.getTestCase()

        val page = (result as UseCaseResult.Success).result.model.single()
        assertEquals("", page.title)
        val section = page.items.single() as SectionDomainModel
        assertEquals("Section", section.title)
        val choice = section.items.single() as ChoiceQuestionDomainModel
        assertTrue(choice.responseSet.multipleSelection)
        assertEquals("A", choice.responseSet.responses.single().label)
    }

    @Test
    fun `network error maps to use case error`() = runTest {
        val repository = LumiformTestRepositoryImpl(FakeService(NetworkResult.NetworkError(Exception("boom"))))

        val result = repository.getTestCase()

        assertEquals(UseCaseError.NetworkError("boom"), (result as UseCaseResult.Error).error)
    }

    private class FakeService(
        private val result: NetworkResult<List<PageDto>>
    ) : LumiformTestService {
        override suspend fun getTestCase() = result
    }
}
