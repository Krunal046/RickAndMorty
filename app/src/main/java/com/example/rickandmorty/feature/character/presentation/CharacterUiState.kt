package com.example.rickandmorty.feature.character.presentation

import androidx.annotation.StringRes
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * What the screen renders. `Loading` lives here rather than in Resource because it
 * describes the UI, not an API response.
 */
sealed interface CharacterUiState {

    data object Loading : CharacterUiState

    data class Success(val characters: List<CharacterModel>) : CharacterUiState

    data class Error(@StringRes val messageRes: Int) : CharacterUiState
}
