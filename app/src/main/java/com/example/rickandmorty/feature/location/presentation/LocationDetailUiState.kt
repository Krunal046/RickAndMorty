package com.example.rickandmorty.feature.location.presentation

import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.location.domain.model.LocationModel

/**
 * Everything the location detail screen renders.
 *
 * [residentsError] is separate from [error] because the two sections fail independently -
 * the batch call for the residents going down is no reason to take the location off screen.
 */
data class LocationDetailUiState(
    val location: LocationModel? = null,
    /** Spec L4. Read from the cache, so the grid survives going offline. */
    val residents: List<CharacterModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: DataError? = null,
    val residentsError: DataError? = null
) {

    /**
     * The blocking spinner, and only when there is genuinely nothing to show instead - the
     * same rule `PagedContent` applies to the lists.
     */
    val isLoading: Boolean get() = location == null && error == null

    /** A failed refresh is fatal only with an empty cache behind it; otherwise it is a banner. */
    val isBlockingError: Boolean get() = location == null && error != null
}
