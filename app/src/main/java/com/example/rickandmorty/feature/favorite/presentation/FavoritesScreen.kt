package com.example.rickandmorty.feature.favorite.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.rickandmorty.R
import com.example.rickandmorty.core.ui.EmptyState
import com.example.rickandmorty.core.ui.LoadingState
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.presentation.CharacterRow

/**
 * Spec S8. Stateless, and reuses the character card the list screen uses so a character
 * reads the same wherever it appears.
 *
 * No pull-to-refresh and no error state: there is nothing to fetch. The tab works offline
 * by construction, which is the whole point of X3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onEvent: (FavoritesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_favorites)) }) }
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            uiState.isLoading -> LoadingState(modifier = content)

            uiState.isEmpty -> EmptyState(
                messageRes = R.string.favorites_empty,
                modifier = content
            )

            else -> FavoriteList(
                characters = uiState.characters,
                onEvent = onEvent,
                modifier = content
            )
        }
    }
}

@Composable
private fun FavoriteList(
    characters: List<CharacterModel>,
    onEvent: (FavoritesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items = characters, key = { it.id }) { character ->
            CharacterRow(
                character = character,
                onClick = { onEvent(FavoritesUiEvent.CharacterClicked(it)) },
                trailing = {
                    IconButton(
                        onClick = { onEvent(FavoritesUiEvent.FavoriteToggled(character.id)) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.action_unfavorite),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            HorizontalDivider()
        }
    }
}
