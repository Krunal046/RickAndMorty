package com.example.rickandmorty.feature.episode.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.navigation.Route
import com.example.rickandmorty.feature.character.domain.usecase.GetCharactersByIdsUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharactersUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.ObserveEpisodeUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.RefreshEpisodeUseCase
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
 * Spec E2 and E4: one episode, read from the cache, plus the characters featured in it.
 *
 * The same shape as `CharacterDetailViewModel`, and for the same reasons: the screen is fed
 * entirely from the database, and a refresh is a background write whose failure only raises
 * a banner while something is cached.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpisodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEpisode: ObserveEpisodeUseCase,
    getCharactersByIds: GetCharactersByIdsUseCase,
    private val refreshEpisode: RefreshEpisodeUseCase,
    private val refreshCharacters: RefreshCharactersUseCase
) : ViewModel() {

    /** Read back off the type-safe route, so there is no argument key to keep in sync. */
    private val episodeId: Int = savedStateHandle.toRoute<Route.EpisodeDetail>().episodeId

    private val _uiState = MutableStateFlow(EpisodeDetailUiState())
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EpisodeDetailUiEffect>(Channel.BUFFERED)
    val effects: Flow<EpisodeDetailUiEffect> = _effects.receiveAsFlow()

    init {
        observeEpisode(episodeId)
            .onEach { episode -> _uiState.update { it.copy(episode = episode) } }
            .launchIn(viewModelScope)

        // Who is in the cast is a property of the episode, so it is derived from the episode
        // rather than passed in: the ids are not known until the cache holds a copy, and they
        // change under us when a refresh brings in a new one.
        val castIds = _uiState
            .map { it.episode?.characterIds.orEmpty() }
            .distinctUntilChanged()

        castIds
            .flatMapLatest { ids -> getCharactersByIds(ids) }
            .onEach { cast -> _uiState.update { it.copy(cast = cast) } }
            .launchIn(viewModelScope)

        // collectLatest so a batch still in flight is abandoned when the ids change.
        viewModelScope.launch {
            castIds.collectLatest { ids -> refreshCastFor(ids) }
        }

        // Spec's refresh policy: always refresh on screen open.
        refresh()
    }

    fun onEvent(event: EpisodeDetailUiEvent) {
        when (event) {
            EpisodeDetailUiEvent.Refreshed -> refresh()

            EpisodeDetailUiEvent.BackClicked ->
                _effects.trySend(EpisodeDetailUiEffect.NavigateBack)

            is EpisodeDetailUiEvent.CharacterClicked ->
                _effects.trySend(
                    EpisodeDetailUiEffect.NavigateToCharacterDetail(event.characterId)
                )
        }
    }

    /**
     * Refreshing the episode also refreshes its cast: an episode whose character list has
     * changed should not have to wait for the next screen open to show it.
     */
    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val result = refreshEpisode(episodeId)

            _uiState.update {
                it.copy(isRefreshing = false, error = (result as? Resource.Error)?.error)
            }

            refreshCastFor(_uiState.value.episode?.characterIds.orEmpty())
        }
    }

    private suspend fun refreshCastFor(ids: List<Int>) {
        val result = refreshCharacters(ids)

        _uiState.update { it.copy(castError = (result as? Resource.Error)?.error) }
    }
}
