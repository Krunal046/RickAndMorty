package com.example.rickandmorty.feature.episode.presentation

import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery

/**
 * Everything the episode screen renders that Paging does not already own.
 *
 * Loading, error and empty are deliberately absent: they are read from
 * `LazyPagingItems.loadState`, the single source for them, exactly as on the character list.
 */
data class EpisodeUiState(
    val query: EpisodeQuery = EpisodeQuery()
) {
    val isSearching: Boolean get() = !query.isEmpty
}
