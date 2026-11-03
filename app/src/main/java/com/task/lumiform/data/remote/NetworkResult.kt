package com.task.lumiform.data.remote

sealed class NetworkResult<T> {
    class Success<T>(val data: T? = null, val code: Int? = null) : NetworkResult<T>()
    class ApiError<T>(val message: String?, val code: Int) : NetworkResult<T>()
    class NetworkError<T>(val throwable: Throwable) : NetworkResult<T>()
    class NullResult<T> : NetworkResult<T>()
}
