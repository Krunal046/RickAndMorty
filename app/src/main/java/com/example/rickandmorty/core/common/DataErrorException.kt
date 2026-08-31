package com.example.rickandmorty.core.common

/**
 * Adapts a [DataError] to the [Throwable] that Paging's `LoadResult.Error` requires, so a
 * paged load reports failures in the same vocabulary as every other call in the app.
 *
 * The presentation layer unwraps it again in `Throwable.toMessageRes()`.
 */
class DataErrorException(val error: DataError) : Exception()
