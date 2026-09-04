package com.example.rickandmorty.feature.character.presentation

import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender

/**
 * Every interaction that can reach the ViewModel. The UI never calls a specific ViewModel
 * method; it sends an event and waits for state or an effect to come back.
 *
 * Loading, retrying and refreshing the list are absent on purpose: they are
 * `LazyPagingItems.retry()` / `refresh()`, which have to be called on the instance that
 * lives in the composition, and Paging starts the first load itself when the UI collects.
 *
 * The filter events carry the value that was tapped rather than the value it should become,
 * so selecting an already-selected chip is what clears it - the ViewModel owns that rule,
 * not the composable.
 */
sealed interface CharacterUiEvent {

    data class CharacterClicked(val characterId: Int) : CharacterUiEvent

    data class SearchChanged(val name: String) : CharacterUiEvent

    data class StatusToggled(val status: CharacterStatus) : CharacterUiEvent

    data class GenderToggled(val gender: Gender) : CharacterUiEvent

    data class SpeciesToggled(val species: String) : CharacterUiEvent

    data object FiltersToggled : CharacterUiEvent

    data object FiltersCleared : CharacterUiEvent
}
