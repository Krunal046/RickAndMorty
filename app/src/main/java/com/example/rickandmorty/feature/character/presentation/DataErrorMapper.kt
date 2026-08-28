package com.example.rickandmorty.feature.character.presentation

import androidx.annotation.StringRes
import com.example.rickandmorty.R
import com.example.rickandmorty.core.common.DataError

/**
 * Wording lives in the presentation layer so the data layer stays free of
 * user-facing, localizable strings.
 */
@StringRes
fun DataError.toMessageRes(): Int = when (this) {
    DataError.NoInternet -> R.string.error_no_internet
    DataError.Timeout -> R.string.error_timeout
    DataError.Serialization -> R.string.error_serialization
    is DataError.Http -> when (code) {
        404 -> R.string.error_not_found
        in 500..599 -> R.string.error_server
        else -> R.string.error_unknown
    }
    is DataError.Unknown -> R.string.error_unknown
}
