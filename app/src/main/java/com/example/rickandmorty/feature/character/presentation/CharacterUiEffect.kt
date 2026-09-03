package com.example.rickandmorty.feature.character.presentation

/**
 * One-shot results of an event that are not part of the rendered state - navigation today,
 * transient messages later.
 *
 * They are separate from state because they must fire exactly once: a navigation held in a
 * state object would replay on the next recomposition or configuration change and push the
 * detail screen a second time.
 *
 * The ViewModel decides *that* navigation should happen; the composable decides *how*, so no
 * feature ends up holding a NavController.
 */
sealed interface CharacterUiEffect {

    data class NavigateToCharacterDetail(val characterId: Int) : CharacterUiEffect
}
