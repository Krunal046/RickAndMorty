package com.example.rickandmorty.feature.episode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems

/**
 * Binds the episode list to its ViewModel and to navigation, so [EpisodeScreen] stays a pure
 * function of its inputs - previewable, and testable without Hilt or a NavController.
 */
@Composable
fun EpisodeRoute(
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodes = viewModel.episodes.collectAsLazyPagingItems()

    LaunchedEffect(viewModel, onEpisodeClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EpisodeUiEffect.NavigateToEpisodeDetail -> onEpisodeClick(effect.episodeId)
            }
        }
    }

    EpisodeScreen(
        uiState = uiState,
        episodes = episodes,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
