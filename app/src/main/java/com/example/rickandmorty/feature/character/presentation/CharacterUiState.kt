package com.example.rickandmorty.feature.character.presentation

import androidx.annotation.StringRes
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * Everything the character screen renders, and nothing else. No API/DB concepts live
 * here: the ViewModel translates those into these flags before the UI ever sees them.
 *
 * The four render cases the screen cares about are derived rather than stored, so they
 * can never contradict each other:
 *  - [isLoading]        first load / retry with nothing on screen yet
 *  - [showErrorState]   the request failed and there is nothing to fall back to
 *  - [isEmpty]          the request succeeded but returned no characters
 *  - [hasCharacters]    there is a list to draw
 */
data class CharacterUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val characters: List<CharacterModel> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null,
    val selectedCharacterId: Int? = null
) {

    val hasCharacters: Boolean
        get() = characters.isNotEmpty()

    val isEmpty: Boolean
        get() = !isLoading && errorMessageRes == null && characters.isEmpty()

    /** Full-screen error. A failed refresh over an existing list is a message, not a state. */
    val showErrorState: Boolean
        get() = !isLoading && errorMessageRes != null && characters.isEmpty()

    /** Failure while a list is already on screen — show it as a snackbar/banner instead. */
    val transientErrorRes: Int?
        get() = errorMessageRes.takeIf { hasCharacters }
}
