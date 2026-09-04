package com.example.rickandmorty.feature.location.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Binds the location detail to its ViewModel and to navigation, so [LocationDetailScreen]
 * stays a pure function of its inputs.
 *
 * The location id is not a parameter: the ViewModel reads it off the type-safe route from
 * its `SavedStateHandle`, which is also what makes it survive process death.
 */
@Composable
fun LocationDetailRoute(
    onBackClick: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onBackClick, onCharacterClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LocationDetailUiEffect.NavigateBack -> onBackClick()
                is LocationDetailUiEffect.NavigateToCharacterDetail ->
                    onCharacterClick(effect.characterId)
            }
        }
    }

    LocationDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
