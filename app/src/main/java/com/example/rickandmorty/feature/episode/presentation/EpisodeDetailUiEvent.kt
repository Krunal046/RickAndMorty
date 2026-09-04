package com.example.rickandmorty.feature.episode.presentation

/**
 * Every interaction the episode detail screen can send.
 *
 * Retrying a failed load and pulling to refresh are the same request, so they are the same
 * event: both mean "go and fetch this episode again".
 */
sealed interface EpisodeDetailUiEvent {

    data object Refreshed : EpisodeDetailUiEvent

    data object BackClicked : EpisodeDetailUiEvent

    /** A face in the cast grid; opens S3 (spec E4). */
    data class CharacterClicked(val characterId: Int) : EpisodeDetailUiEvent
}
