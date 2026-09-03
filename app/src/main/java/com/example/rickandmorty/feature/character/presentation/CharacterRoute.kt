package com.example.rickandmorty.feature.character.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems

/**
 * Binds the character screen to its ViewModel and to navigation.
 *
 * Keeping this separate is what lets [CharacterScreen] stay a pure function of its inputs -
 * previewable, and testable without Hilt or a NavController.
 */
@Composable
fun CharacterRoute(
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterViewModel = hiltViewModel()
) {
    val characters = viewModel.characters.collectAsLazyPagingItems()

    LaunchedEffect(viewModel, onCharacterClick) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CharacterUiEffect.NavigateToCharacterDetail ->
                    onCharacterClick(effect.characterId)
            }
        }
    }

    CharacterScreen(
        characters = characters,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
