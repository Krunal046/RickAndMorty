package com.example.rickandmorty.feature.favorite.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.feature.favorite.domain.usecase.ObserveFavoriteCharactersUseCase
import com.example.rickandmorty.feature.favorite.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Spec S8. The simplest screen in the app: it observes one query and has nothing to
 * refresh, because favorites never come from the network.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavoriteCharacters: ObserveFavoriteCharactersUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FavoritesUiEffect>(Channel.BUFFERED)
    val effects: Flow<FavoritesUiEffect> = _effects.receiveAsFlow()

    init {
        observeFavoriteCharacters()
            .onEach { characters ->
                _uiState.update { it.copy(characters = characters, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: FavoritesUiEvent) {
        when (event) {
            is FavoritesUiEvent.CharacterClicked ->
                _effects.trySend(FavoritesUiEffect.NavigateToCharacterDetail(event.characterId))

            // No optimistic removal: the row leaves the list when Room says it has, which is
            // the same flow the tab was already rendering.
            is FavoritesUiEvent.FavoriteToggled ->
                viewModelScope.launch { toggleFavorite(event.characterId) }
        }
    }
}
