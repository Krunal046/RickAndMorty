package com.example.rickandmorty.feature.location.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems

/**
 * Binds the location list to its ViewModel and to navigation, so [LocationScreen] stays a
 * pure function of its inputs.
 */
@Composable
fun LocationRoute(
    onLocationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locations = viewModel.locations.collectAsLazyPagingItems()

    LaunchedEffect(viewModel, onLocationClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LocationUiEffect.NavigateToLocationDetail -> onLocationClick(effect.locationId)
            }
        }
    }

    LocationScreen(
        uiState = uiState,
        locations = locations,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
