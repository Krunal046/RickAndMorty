package com.example.rickandmorty.feature.episode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Binds the episode detail to its ViewModel and to navigation, so [EpisodeDetailScreen]
 * stays a pure function of its inputs.
 *
 * The episode id is not a parameter: the ViewModel reads it off the type-safe route from its
 * `SavedStateHandle`, which is also what makes it survive process death.
 */
@Composable
fun EpisodeDetailRoute(
    onBackClick: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onBackClick, onCharacterClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EpisodeDetailUiEffect.NavigateBack -> onBackClick()
                is EpisodeDetailUiEffect.NavigateToCharacterDetail ->
                    onCharacterClick(effect.characterId)
            }
        }
    }

    EpisodeDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
