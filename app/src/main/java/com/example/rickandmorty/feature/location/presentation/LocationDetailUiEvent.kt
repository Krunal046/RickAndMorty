package com.example.rickandmorty.feature.location.presentation

/**
 * Every interaction the location detail screen can send.
 *
 * Retrying a failed load and pulling to refresh are the same request, so they are the same
 * event: both mean "go and fetch this location again".
 */
sealed interface LocationDetailUiEvent {

    data object Refreshed : LocationDetailUiEvent

    data object BackClicked : LocationDetailUiEvent

    /** A resident; opens S3 (spec L4). */
    data class CharacterClicked(val characterId: Int) : LocationDetailUiEvent
}
