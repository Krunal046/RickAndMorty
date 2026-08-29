package com.example.rickandmorty.feature.character.presentation

/**
 * Every interaction that can reach the ViewModel — user taps, screen lifecycle, and any
 * work that follows from them (network, local DB). The UI never calls a specific
 * ViewModel method; it sends an event and waits for [CharacterUiState] to change.
 */
sealed interface CharacterUiEvent {

    /** Screen entered / first load. */
    data object LoadCharacters : CharacterUiEvent

    /** User tapped "Try again" on the error state. */
    data object Retry : CharacterUiEvent

    /** Pull-to-refresh over an existing list. */
    data object Refresh : CharacterUiEvent

    data class CharacterClicked(val characterId: Int) : CharacterUiEvent

    /** Detail closed — clears the selection. */
    data object SelectionCleared : CharacterUiEvent

    /** Transient error message has been shown, so it can be dropped from state. */
    data object ErrorShown : CharacterUiEvent
}
