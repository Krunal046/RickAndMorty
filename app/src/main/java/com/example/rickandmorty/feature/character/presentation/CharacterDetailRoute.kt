package com.example.rickandmorty.feature.character.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Binds the detail screen to its ViewModel and to navigation, so [CharacterDetailScreen]
 * stays a pure function of its inputs.
 *
 * The character id is not a parameter: the ViewModel reads it off the type-safe route from
 * its `SavedStateHandle`, which is also what makes it survive process death.
 */
@Composable
fun CharacterDetailRoute(
    onBackClick: () -> Unit,
    onEpisodeClick: (Int) -> Unit,
    onLocationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onBackClick, onEpisodeClick, onLocationClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CharacterDetailUiEffect.NavigateBack -> onBackClick()
                is CharacterDetailUiEffect.NavigateToEpisode -> onEpisodeClick(effect.episodeId)
                is CharacterDetailUiEffect.NavigateToLocation -> onLocationClick(effect.locationId)
            }
        }
    }

    CharacterDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
