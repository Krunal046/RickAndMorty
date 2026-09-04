package com.example.rickandmorty.feature.character.presentation

/** One-shot navigation results. See `CharacterUiEffect` for why these are not state. */
sealed interface CharacterDetailUiEffect {

    data object NavigateBack : CharacterDetailUiEffect

    data class NavigateToEpisode(val episodeId: Int) : CharacterDetailUiEffect

    data class NavigateToLocation(val locationId: Int) : CharacterDetailUiEffect
}
