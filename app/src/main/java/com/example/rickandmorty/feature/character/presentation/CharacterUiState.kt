package com.example.rickandmorty.feature.character.presentation

import com.example.rickandmorty.feature.character.domain.model.CharacterQuery

/**
 * Everything the character screen renders that Paging does not already own.
 *
 * Loading, error and empty are deliberately absent: they are read from
 * `LazyPagingItems.loadState`, which is the single source for them. Duplicating them here
 * would give the screen two answers to "am I loading?" that could disagree.
 */
data class CharacterUiState(
    val query: CharacterQuery = CharacterQuery(),
    /** The filter panel is collapsed by default so the list gets the room. */
    val filtersExpanded: Boolean = false
) {
    val isSearching: Boolean get() = !query.isEmpty
}
