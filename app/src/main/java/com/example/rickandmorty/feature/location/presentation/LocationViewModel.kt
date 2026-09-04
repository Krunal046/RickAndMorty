package com.example.rickandmorty.feature.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import com.example.rickandmorty.feature.location.domain.usecase.GetLocationPagingUseCase
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
class LocationViewModel @Inject constructor(
    getLocationPaging: GetLocationPagingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    /**
     * The list is a function of the query, so changing a filter rebuilds the pager rather
     * than filtering what is already loaded - the API does the filtering and each result set
     * is cached under its own key.
     *
     * The debounce waits out typing so a name is one request rather than one per keystroke,
     * but only while there is text: clearing the box or toggling a chip applies at once, and
     * the first load is not held up.
     */
    val locations: Flow<PagingData<LocationModel>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .debounce { query -> if (query.name.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { query -> getLocationPaging(query) }
        .cachedIn(viewModelScope)

    private val _effects = Channel<LocationUiEffect>(Channel.BUFFERED)
    val effects: Flow<LocationUiEffect> = _effects.receiveAsFlow()

    fun onEvent(event: LocationUiEvent) {
        when (event) {
            is LocationUiEvent.LocationClicked ->
                _effects.trySend(LocationUiEffect.NavigateToLocationDetail(event.locationId))

            is LocationUiEvent.SearchChanged -> updateQuery { it.copy(name = event.name) }

            // Tapping the selected chip again clears that filter.
            is LocationUiEvent.TypeToggled -> updateQuery {
                it.copy(type = if (it.type == event.type) "" else event.type)
            }

            is LocationUiEvent.DimensionToggled -> updateQuery {
                it.copy(dimension = if (it.dimension == event.dimension) "" else event.dimension)
            }

            LocationUiEvent.FiltersToggled ->
                _uiState.update { it.copy(filtersExpanded = !it.filtersExpanded) }

            // Clears the filters but keeps the search text, which is what the button says.
            LocationUiEvent.FiltersCleared -> updateQuery {
                it.copy(type = "", dimension = "")
            }
        }
    }

    private fun updateQuery(transform: (LocationQuery) -> LocationQuery) {
        _uiState.update { it.copy(query = transform(it.query)) }
    }

    private companion object {
        /** Long enough to swallow a burst of typing, short enough not to feel laggy. */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
