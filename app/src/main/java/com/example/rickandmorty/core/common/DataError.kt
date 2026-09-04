package com.example.rickandmorty.core.common

/**
 * Typed failure reasons. Intentionally free of user-facing strings so that the
 * presentation layer owns wording and localization.
 */
sealed interface DataError {

    data object NoInternet : DataError

    data object Timeout : DataError

    data class Http(val code: Int) : DataError

    data object Serialization : DataError

    data class Unknown(val throwable: Throwable) : DataError
}
