package com.example.rickandmorty.feature.character.presentation

/**
 * Every interaction the detail screen can send.
 *
 * Retrying a failed load and pulling to refresh are the same request, so they are the same
 * event: both mean "go and fetch this character again".
 */
sealed interface CharacterDetailUiEvent {

    data object Refreshed : CharacterDetailUiEvent

    data object BackClicked : CharacterDetailUiEvent

    /** Saves the character, or removes it if it is already saved (spec X3). */
    data object FavoriteToggled : CharacterDetailUiEvent

    data class EpisodeClicked(val episodeId: Int) : CharacterDetailUiEvent

    /** Origin or last known location; both open the same screen (spec S7). */
    data class LocationClicked(val locationId: Int) : CharacterDetailUiEvent
}
