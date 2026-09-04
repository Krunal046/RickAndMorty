package com.example.rickandmorty.feature.episode.presentation

/**
 * One-shot results of an event that are not part of the rendered state.
 *
 * Separate from state because they must fire exactly once: a navigation held in a state
 * object would replay on the next recomposition and push the detail screen a second time.
 */
sealed interface EpisodeUiEffect {

    data class NavigateToEpisodeDetail(val episodeId: Int) : EpisodeUiEffect
}
