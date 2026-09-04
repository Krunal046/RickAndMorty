package com.example.rickandmorty.feature.favorite.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Binds the Favorites tab to its ViewModel and to navigation. */
@Composable
fun FavoritesRoute(
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onCharacterClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FavoritesUiEffect.NavigateToCharacterDetail ->
                    onCharacterClick(effect.characterId)
            }
        }
    }

    FavoritesScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
