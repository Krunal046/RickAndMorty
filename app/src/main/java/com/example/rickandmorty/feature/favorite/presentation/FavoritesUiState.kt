package com.example.rickandmorty.feature.favorite.presentation

import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * Everything the Favorites tab renders.
 *
 * There is no error state, and that is not an omission: spec X3 has no network path, so the
 * only things that can happen here are "the database has not answered yet", "it answered
 * with nothing" and "it answered with characters".
 */
data class FavoritesUiState(
    val characters: List<CharacterModel> = emptyList(),
    /** True until Room's first emission, so an empty tab does not flash before the rows. */
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && characters.isEmpty()
}
