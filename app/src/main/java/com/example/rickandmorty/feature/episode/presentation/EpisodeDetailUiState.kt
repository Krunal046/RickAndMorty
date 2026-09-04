package com.example.rickandmorty.feature.episode.presentation

import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * Everything the episode detail screen renders.
 *
 * Mirrors `CharacterDetailUiState`, including the reason it owns its own loading and error
 * state: those come from Paging's `loadState` only where there is a `LazyPagingItems` to
 * read them from, and a detail is a single row, not a page.
 *
 * [castError] is separate from [error] because the two sections fail independently - the
 * batch call for the cast going down is no reason to take the episode off screen.
 */
data class EpisodeDetailUiState(
    val episode: EpisodeModel? = null,
    /** Spec E4. Read from the cache, so the grid survives going offline. */
    val cast: List<CharacterModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: DataError? = null,
    val castError: DataError? = null
) {

    /**
     * The blocking spinner, and only when there is genuinely nothing to show instead - the
     * same rule `PagedContent` applies to the lists.
     */
    val isLoading: Boolean get() = episode == null && error == null

    /** A failed refresh is fatal only with an empty cache behind it; otherwise it is a banner. */
    val isBlockingError: Boolean get() = episode == null && error != null
}
