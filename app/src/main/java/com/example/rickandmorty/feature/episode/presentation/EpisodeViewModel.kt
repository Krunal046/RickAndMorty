package com.example.rickandmorty.feature.episode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import com.example.rickandmorty.feature.episode.domain.usecase.GetEpisodePagingUseCase
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
class EpisodeViewModel @Inject constructor(
    getEpisodePaging: GetEpisodePagingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    /**
     * The list is a function of the query, so searching rebuilds the pager rather than
     * filtering what is already loaded - the API does the filtering and each result set is
     * cached under its own key. Same debounce rule as the character list: typing is held for
     * a moment, clearing the box and the first load are not.
     *
     * The season headings (spec E1) are inserted here rather than in the data layer because
     * they are not data - nothing in the database corresponds to a heading, and the screen
     * below has no way to group a list it only ever sees a page of.
     */
    val episodes: Flow<PagingData<EpisodeListItem>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .debounce { query -> if (query.isEmpty) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { query -> getEpisodePaging(query) }
        .map { pagingData -> pagingData.withSeasonHeaders() }
        .cachedIn(viewModelScope)

    private val _effects = Channel<EpisodeUiEffect>(Channel.BUFFERED)
    val effects: Flow<EpisodeUiEffect> = _effects.receiveAsFlow()

    fun onEvent(event: EpisodeUiEvent) {
        when (event) {
            is EpisodeUiEvent.SearchChanged ->
                _uiState.update { it.copy(query = EpisodeQuery(event.search)) }

            is EpisodeUiEvent.EpisodeClicked ->
                _effects.trySend(EpisodeUiEffect.NavigateToEpisodeDetail(event.episodeId))
        }
    }

    private companion object {
        /** Long enough to swallow a burst of typing, short enough not to feel laggy. */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

/**
 * Turns a page of episodes into a page of list items with a heading wherever the season
 * changes.
 *
 * `insertSeparators` sees neighbours across page boundaries, so the first episode of season
 * two gets its heading even when it lands in a page the season-one episodes are not in - the
 * reason grouping cannot be done a page at a time.
 *
 * `before` is null only at the very start of the list, which is why the leading heading is
 * emitted the same way as every other one rather than special-cased.
 */
private fun PagingData<EpisodeModel>.withSeasonHeaders(): PagingData<EpisodeListItem> =
    map<_, EpisodeListItem> { EpisodeListItem.Item(it) }
        .insertSeparators { before, after ->
            when {
                // End of the list: a heading with nothing under it would be a lie.
                after == null -> null

                before == null || before.seasonOrNull() != after.seasonOrNull() ->
                    EpisodeListItem.Header(after.seasonOrNull())

                else -> null
            }
        }

/** Only [EpisodeListItem.Item]s exist at the point separators are generated. */
private fun EpisodeListItem.seasonOrNull(): Int? = (this as? EpisodeListItem.Item)?.episode?.season
