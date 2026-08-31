package com.example.rickandmorty.feature.character.presentation

/**
 * Every interaction that can reach the ViewModel — user taps, screen lifecycle, and any
 * work that follows from them (network, local DB). The UI never calls a specific
 * ViewModel method; it sends an event and waits for [CharacterUiState] to change.
 *
 * Loading, retrying and refreshing the list are absent on purpose: they are
 * `LazyPagingItems.retry()` / `refresh()`, which have to be called on the instance that
 * lives in the composition, and Paging starts the first load itself when the UI collects.
 */
sealed interface CharacterUiEvent {

    data class CharacterClicked(val characterId: Int) : CharacterUiEvent

    /** Detail closed — clears the selection. */
    data object SelectionCleared : CharacterUiEvent
}
