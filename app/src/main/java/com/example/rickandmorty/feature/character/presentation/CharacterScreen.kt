package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.rickandmorty.R
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

@Composable
fun CharacterScreen(
    uiState: CharacterUiState,
    characters: LazyPagingItems<CharacterModel>,
    onEvent: (CharacterUiEvent) -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val refresh = characters.loadState.refresh

        when {
            refresh is LoadState.Loading -> LoadingState(Modifier.padding(innerPadding))

            // A refresh failure means there is nothing to fall back to: block the screen.
            refresh is LoadState.Error -> ErrorState(
                messageRes = refresh.error.toMessageRes(),
                onRetry = characters::retry,
                modifier = Modifier.padding(innerPadding)
            )

            // endOfPaginationReached keeps the empty state from flashing before the
            // first page has landed.
            characters.itemCount == 0 &&
                    characters.loadState.append.endOfPaginationReached ->
                EmptyState(Modifier.padding(innerPadding))

            else -> CharacterList(
                characters = characters,
                onCharacterClick = { onEvent(CharacterUiEvent.CharacterClicked(it)) },
                contentPadding = innerPadding
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    messageRes: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.action_retry))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.characters_empty),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CharacterList(
    characters: LazyPagingItems<CharacterModel>,
    onCharacterClick: (Int) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(
            count = characters.itemCount,
            key = characters.itemKey { it.id },
            contentType = characters.itemContentType { CHARACTER_CONTENT_TYPE }
        ) { index ->
            // Null only with placeholders enabled, which this list does not use.
            characters[index]?.let { character ->
                CharacterRow(character = character, onCharacterClick = onCharacterClick)
            }
        }

        // A failure while a list is already on screen is a footer, not a full-screen error.
        when (val append = characters.loadState.append) {
            is LoadState.Loading -> item { AppendLoading() }

            is LoadState.Error -> item {
                AppendError(
                    messageRes = append.error.toMessageRes(),
                    onRetry = characters::retry
                )
            }

            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun CharacterRow(
    character: CharacterModel,
    onCharacterClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCharacterClick(character.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = character.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${character.status} • ${character.species}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AppendLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppendError(
    messageRes: Int,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.action_retry))
        }
    }
}

private const val CHARACTER_CONTENT_TYPE = "character"
