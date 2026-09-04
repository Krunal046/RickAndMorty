package com.example.rickandmorty.feature.location.presentation

/**
 * Every interaction that can reach the location list's ViewModel.
 *
 * The filter events carry the value that was tapped rather than the value it should become,
 * so selecting an already-selected chip is what clears it - the ViewModel owns that rule,
 * not the composable.
 */
sealed interface LocationUiEvent {

    data class SearchChanged(val name: String) : LocationUiEvent

    data class TypeToggled(val type: String) : LocationUiEvent

    data class DimensionToggled(val dimension: String) : LocationUiEvent

    data object FiltersToggled : LocationUiEvent

    data object FiltersCleared : LocationUiEvent

    data class LocationClicked(val locationId: Int) : LocationUiEvent
}
