package com.example.rickandmorty.feature.character.presentation

import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * Everything the character detail screen renders.
 *
 * Unlike the list, this screen owns its loading and error state: those come from Paging's
 * `loadState` only where there is a `LazyPagingItems` to read them from, and a detail is a
 * single row, not a page.
 *
 * The errors are kept as [DataError] rather than a string so the wording still lives in
 * `DataErrorMapper` and the ViewModel stays free of resources.
 *
 * [episodesError] is separate from [error] because the two sections fail independently: the
 * batch call for the episode chips going down is no reason to take the character off screen.
 */
data class CharacterDetailUiState(
    val character: CharacterModel? = null,
    val episodes: List<EpisodeModel> = emptyList(),
    val isRefreshing: Boolean = false,
    /** Spec X3. Read from the favorites table, not from anything the network said. */
    val isFavorite: Boolean = false,
    val error: DataError? = null,
    val episodesError: DataError? = null
) {

    /**
     * The blocking spinner, and only when there is genuinely nothing to show instead - the
     * same rule `PagedContent` applies to the lists.
     */
    val isLoading: Boolean get() = character == null && error == null

    /** A failed refresh is fatal only with an empty cache behind it; otherwise it is a banner. */
    val isBlockingError: Boolean get() = character == null && error != null
}
