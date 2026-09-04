package com.example.rickandmorty.feature.episode.presentation

/**
 * One-shot results of an event, kept out of state so they fire exactly once.
 *
 * The ViewModel decides *that* navigation should happen; the composable decides *how*, so no
 * feature ends up holding a NavController.
 */
sealed interface EpisodeDetailUiEffect {

    data object NavigateBack : EpisodeDetailUiEffect

    data class NavigateToCharacterDetail(val characterId: Int) : EpisodeDetailUiEffect
}
