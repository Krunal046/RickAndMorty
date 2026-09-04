package com.example.rickandmorty.feature.location.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.navigation.Route
import com.example.rickandmorty.feature.character.domain.usecase.GetCharactersByIdsUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharactersUseCase
import com.example.rickandmorty.feature.location.domain.usecase.ObserveLocationUseCase
import com.example.rickandmorty.feature.location.domain.usecase.RefreshLocationUseCase
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
 * Spec L2 and L4: one location, read from the cache, plus the characters who live there.
 *
 * The same shape as the other two detail screens. This is the one most often opened from
 * somewhere else - a character's origin or last-location link (spec C3) - so the detail
 * cache key matters here: the location behind that link may be in no list the user has ever
 * scrolled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeLocation: ObserveLocationUseCase,
    getCharactersByIds: GetCharactersByIdsUseCase,
    private val refreshLocation: RefreshLocationUseCase,
    private val refreshCharacters: RefreshCharactersUseCase
) : ViewModel() {

    /** Read back off the type-safe route, so there is no argument key to keep in sync. */
    private val locationId: Int = savedStateHandle.toRoute<Route.LocationDetail>().locationId

    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LocationDetailUiEffect>(Channel.BUFFERED)
    val effects: Flow<LocationDetailUiEffect> = _effects.receiveAsFlow()

    init {
        observeLocation(locationId)
            .onEach { location -> _uiState.update { it.copy(location = location) } }
            .launchIn(viewModelScope)

        // Who lives here is a property of the location, so it is derived from the location
        // rather than passed in: the ids are not known until the cache holds a copy.
        val residentIds = _uiState
            .map { it.location?.residentIds.orEmpty() }
            .distinctUntilChanged()

        residentIds
            .flatMapLatest { ids -> getCharactersByIds(ids) }
            .onEach { residents -> _uiState.update { it.copy(residents = residents) } }
            .launchIn(viewModelScope)

        // collectLatest so a batch still in flight is abandoned when the ids change.
        viewModelScope.launch {
            residentIds.collectLatest { ids -> refreshResidentsFor(ids) }
        }

        // Spec's refresh policy: always refresh on screen open.
        refresh()
    }

    fun onEvent(event: LocationDetailUiEvent) {
        when (event) {
            LocationDetailUiEvent.Refreshed -> refresh()

            LocationDetailUiEvent.BackClicked ->
                _effects.trySend(LocationDetailUiEffect.NavigateBack)

            is LocationDetailUiEvent.CharacterClicked ->
                _effects.trySend(
                    LocationDetailUiEffect.NavigateToCharacterDetail(event.characterId)
                )
        }
    }

    /**
     * Refreshing the location also refreshes its residents: a location whose population has
     * changed should not have to wait for the next screen open to show it.
     */
    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val result = refreshLocation(locationId)

            _uiState.update {
                it.copy(isRefreshing = false, error = (result as? Resource.Error)?.error)
            }

            refreshResidentsFor(_uiState.value.location?.residentIds.orEmpty())
        }
    }

    private suspend fun refreshResidentsFor(ids: List<Int>) {
        val result = refreshCharacters(ids)

        _uiState.update { it.copy(residentsError = (result as? Resource.Error)?.error) }
    }
}
