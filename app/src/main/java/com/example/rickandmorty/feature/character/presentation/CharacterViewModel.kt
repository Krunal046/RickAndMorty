package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterPagingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CharacterViewModel @Inject constructor(
    getCharacterPaging: GetCharacterPagingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    /**
     * The list is a function of the query, so changing the query rebuilds the pager rather
     * than filtering what is already loaded - the API does the filtering, and each result
     * set is cached under its own key.
     *
     * `debounce` waits out typing so a five-letter name is one request, not five, but only
     * while there is text: clearing the box or toggling a chip with an empty box applies at
     * once, and the very first load is not held up. `flatMapLatest` drops the previous
     * query's pager as soon as a new one arrives.
     */
    val characters: Flow<PagingData<CharacterModel>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .debounce { query -> if (query.name.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { query -> getCharacterPaging(query) }
        .cachedIn(viewModelScope)

    /**
     * Buffered so an effect emitted before the UI starts collecting - during a
     * configuration change, say - is delivered rather than dropped.
     */
    private val _effects = Channel<CharacterUiEffect>(Channel.BUFFERED)
    val effects: Flow<CharacterUiEffect> = _effects.receiveAsFlow()

    /** The single entry point for the UI. */
    fun onEvent(event: CharacterUiEvent) {
        when (event) {
            is CharacterUiEvent.CharacterClicked ->
                _effects.trySend(CharacterUiEffect.NavigateToCharacterDetail(event.characterId))

            is CharacterUiEvent.SearchChanged -> updateQuery { it.copy(name = event.name) }

            // Tapping the selected chip again clears that filter.
            is CharacterUiEvent.StatusToggled -> updateQuery {
                it.copy(status = if (it.status == event.status) null else event.status)
            }

            is CharacterUiEvent.GenderToggled -> updateQuery {
                it.copy(gender = if (it.gender == event.gender) null else event.gender)
            }

            is CharacterUiEvent.SpeciesToggled -> updateQuery {
                it.copy(species = if (it.species == event.species) "" else event.species)
            }

            CharacterUiEvent.FiltersToggled ->
                _uiState.update { it.copy(filtersExpanded = !it.filtersExpanded) }

            // Clears the filters but keeps the search text, which is what the button says.
            CharacterUiEvent.FiltersCleared -> updateQuery {
                it.copy(status = null, species = "", gender = null)
            }
        }
    }

    private fun updateQuery(transform: (CharacterQuery) -> CharacterQuery) {
        _uiState.update { it.copy(query = transform(it.query)) }
    }

    private companion object {
        /** Long enough to swallow a burst of typing, short enough not to feel laggy. */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
