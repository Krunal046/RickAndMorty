package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.navigation.Route
import com.example.rickandmorty.feature.character.domain.usecase.ObserveCharacterUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharacterUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.GetEpisodesByIdsUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.RefreshEpisodesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Spec C3 and C4: one character, read from the cache, plus the episodes it appears in.
 *
 * The screen is fed entirely from the database. A refresh is a background write, so a
 * failure while something is cached only raises a banner - the character stays on screen,
 * which is what makes the detail work offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCharacter: ObserveCharacterUseCase,
    getEpisodesByIds: GetEpisodesByIdsUseCase,
    private val refreshCharacter: RefreshCharacterUseCase,
    private val refreshEpisodes: RefreshEpisodesUseCase
) : ViewModel() {

    /** Read back off the type-safe route, so there is no argument key to keep in sync. */
    private val characterId: Int = savedStateHandle.toRoute<Route.CharacterDetail>().characterId

    private val _uiState = MutableStateFlow(CharacterDetailUiState())
    val uiState: StateFlow<CharacterDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CharacterDetailUiEffect>(Channel.BUFFERED)
    val effects: Flow<CharacterDetailUiEffect> = _effects.receiveAsFlow()

    init {
        observeCharacter(characterId)
            .onEach { character -> _uiState.update { it.copy(character = character) } }
            .launchIn(viewModelScope)

        // Which episodes to show is a property of the character, so it is derived from the
        // character rather than passed in: the ids are not known until the cache holds a
        // copy, and they change under us when a refresh brings in a new one.
        val episodeIds = _uiState
            .map { it.character?.episodeIds.orEmpty() }
            .distinctUntilChanged()

        episodeIds
            .flatMapLatest { ids -> getEpisodesByIds(ids) }
            .onEach { episodes -> _uiState.update { it.copy(episodes = episodes) } }
            .launchIn(viewModelScope)

        // collectLatest so a batch still in flight is abandoned when the ids change.
        viewModelScope.launch {
            episodeIds.collectLatest { ids -> refreshEpisodesFor(ids) }
        }

        // Spec's refresh policy: always refresh on screen open.
        refresh()
    }

    fun onEvent(event: CharacterDetailUiEvent) {
        when (event) {
            CharacterDetailUiEvent.Refreshed -> refresh()

            CharacterDetailUiEvent.BackClicked ->
                _effects.trySend(CharacterDetailUiEffect.NavigateBack)

            is CharacterDetailUiEvent.EpisodeClicked ->
                _effects.trySend(CharacterDetailUiEffect.NavigateToEpisode(event.episodeId))

            is CharacterDetailUiEvent.LocationClicked ->
                _effects.trySend(CharacterDetailUiEffect.NavigateToLocation(event.locationId))
        }
    }

    /**
     * Refreshing the character also refreshes its episodes: a character that has just picked
     * up a new episode should not have to wait for the next screen open to show it.
     */
    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val result = refreshCharacter(characterId)

            _uiState.update {
                it.copy(isRefreshing = false, error = (result as? Resource.Error)?.error)
            }

            refreshEpisodesFor(_uiState.value.character?.episodeIds.orEmpty())
        }
    }

    private suspend fun refreshEpisodesFor(ids: List<Int>) {
        val result = refreshEpisodes(ids)

        _uiState.update { it.copy(episodesError = (result as? Resource.Error)?.error) }
    }
}
