package com.task.lumiform.data.mapper

import com.task.lumiform.data.remote.NetworkResult
import com.task.lumiform.domain.usecase.UseCaseError
import com.task.lumiform.domain.usecase.UseCaseResult

const val EMPTY_RESULT_MESSAGE = "Service unavailable, please try again."
const val NETWORK_ERROR_MESSAGE = "Please check your internet connection."

inline fun <T, R> NetworkResult<T>.toUseCaseResult(
    emptyObject: R? = null,
    mapper: (T) -> R
): UseCaseResult<R> = when (this) {
    is NetworkResult.ApiError -> UseCaseResult.Error(
        UseCaseError.ApiError(code = code, message = message.orEmpty())
    )
    // The raw exception message (e.g. "Unable to resolve host") is not meant for the user.
    is NetworkResult.NetworkError -> UseCaseResult.Error(UseCaseError.NetworkError(NETWORK_ERROR_MESSAGE))
    is NetworkResult.NullResult -> emptyObject.toSuccessOrEmptyError()
    is NetworkResult.Success -> data
        ?.let { body -> UseCaseResult.Success(result = mapper(body)) }
        ?: emptyObject.toSuccessOrEmptyError()
}

fun <R> R?.toSuccessOrEmptyError(): UseCaseResult<R> =
    this?.let { UseCaseResult.Success(it) }
        ?: UseCaseResult.Error(UseCaseError.EmptyResultError(EMPTY_RESULT_MESSAGE))
