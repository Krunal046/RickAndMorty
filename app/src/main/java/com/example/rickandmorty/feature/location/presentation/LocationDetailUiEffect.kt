package com.example.rickandmorty.feature.location.presentation

/**
 * One-shot results of an event, kept out of state so they fire exactly once.
 */
sealed interface LocationDetailUiEffect {

    data object NavigateBack : LocationDetailUiEffect

    data class NavigateToCharacterDetail(val characterId: Int) : LocationDetailUiEffect
}
