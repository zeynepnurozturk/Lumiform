package com.task.lumiform.domain.usecase

const val DEFAULT_CODE_SUCCESS = 200
const val DEFAULT_CODE_ERROR = 400


sealed class UseCaseResult<out T> {
    data class Success<T>(val result: T, val code: Int = DEFAULT_CODE_SUCCESS) : UseCaseResult<T>()
    data class Error(val error: UseCaseError) : UseCaseResult<Nothing>() {
        val message: String get() = error.message
    }
    data class Warning(val message: String, val code: Int = DEFAULT_CODE_ERROR) : UseCaseResult<Nothing>()

    fun isSuccessful() = this is Success<*>
    fun isFailure() = !isSuccessful()
}

sealed class UseCaseError(open val message: String) {
    data class NetworkError(override val message: String) : UseCaseError(message)
    data class ApiError(val code: Int, override val message: String) : UseCaseError(message)
    data class EmptyResultError(override val message: String) : UseCaseError(message)
}
