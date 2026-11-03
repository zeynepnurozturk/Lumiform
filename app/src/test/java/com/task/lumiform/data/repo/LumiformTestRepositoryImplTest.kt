package com.task.lumiform.data.repo

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.data.remote.dto.LumiformItemDto
import com.task.lumiform.data.remote.dto.ResponseOptionDto
import com.task.lumiform.data.remote.dto.ResponseSetDto
import com.task.lumiform.data.remote.service.LumiformTestService
import com.task.lumiform.domain.model.LumiformItemDomainModel
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LumiformTestRepositoryImplTest {

    @Test
    fun `success maps dto tree to domain model`() = runTest {
        val dto = LumiformItemDto.Page(
            id = 1,
            title = null,
            items = listOf(
                LumiformItemDto.Choice(
                    id = 2,
                    content = "Question",
                    responseSet = ResponseSetDto(
                        id = 3,
                        multipleSelection = true,
                        responses = listOf(ResponseOptionDto(id = 4, label = "A", score = null))
                    )
                )
            )
        )
        val repository = LumiformTestRepositoryImpl(FakeService(NetworkResult.Success(listOf(dto))))

        val result = repository.getTestCase()

        val page = (result as UseCaseResult.Success).result.model.single() as LumiformItemDomainModel.Page
        assertEquals("", page.title)
        val choice = page.items.single() as LumiformItemDomainModel.Choice
        assertTrue(choice.responseSet!!.multipleSelection)
        assertEquals("A", choice.responseSet.responses.single().label)
    }

    @Test
    fun `network error maps to use case error`() = runTest {
        val repository = LumiformTestRepositoryImpl(FakeService(NetworkResult.NetworkError(Exception("boom"))))

        val result = repository.getTestCase()

        assertEquals(UseCaseError.NetworkError("boom"), (result as UseCaseResult.Error).error)
    }

    private class FakeService(
        private val result: NetworkResult<List<LumiformItemDto>>
    ) : LumiformTestService {
        override suspend fun getTestCase() = result
    }
}
