package com.example.rickandmorty.feature.episode.presentation

/**
 * Every interaction that can reach the episode list's ViewModel.
 *
 * There is one search event rather than one per API parameter: the screen has a single box,
 * and whether the text is a title or an `S01E01` code is `EpisodeQuery`'s decision, not the
 * composable's.
 */
sealed interface EpisodeUiEvent {

    data class SearchChanged(val search: String) : EpisodeUiEvent

    data class EpisodeClicked(val episodeId: Int) : EpisodeUiEvent
}
