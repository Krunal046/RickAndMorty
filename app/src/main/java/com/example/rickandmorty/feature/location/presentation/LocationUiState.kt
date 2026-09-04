package com.example.rickandmorty.feature.location.presentation

import com.example.rickandmorty.feature.location.domain.model.LocationQuery

/**
 * Everything the location screen renders that Paging does not already own.
 *
 * Loading, error and empty are deliberately absent: they are read from
 * `LazyPagingItems.loadState`, the single source for them.
 */
data class LocationUiState(
    val query: LocationQuery = LocationQuery(),
    /** The filter panel is collapsed by default so the list gets the room. */
    val filtersExpanded: Boolean = false
) {
    val isSearching: Boolean get() = !query.isEmpty
}
