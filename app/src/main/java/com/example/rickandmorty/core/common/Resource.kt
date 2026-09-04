package com.example.rickandmorty.core.common

/**
 * Result of an operation that can fail. Returned across the data/domain boundary.
 *
 * There is deliberately no `Loading` case: these are produced by suspend functions,
 * which only return once the work is done. Loading is a UI concern and lives in the
 * screen's UiState instead.
 */
sealed interface Resource<out T> {

    data class Success<out T>(val data: T) : Resource<T>

    data class Error(val error: DataError) : Resource<Nothing>
}
