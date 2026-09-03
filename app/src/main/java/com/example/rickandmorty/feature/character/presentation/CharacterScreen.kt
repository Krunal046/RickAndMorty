package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.rickandmorty.R
import com.example.rickandmorty.core.ui.PagedContent
import com.example.rickandmorty.core.ui.pagingAppendFooter
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * Stateless: everything it renders arrives as a parameter, and everything the user does
 * leaves as a [CharacterUiEvent].
 *
 * Loading, error and empty are not parameters either - [PagedContent] derives them from
 * `LazyPagingItems.loadState`, the single source for them.
 */
@Composable
fun CharacterScreen(
    characters: LazyPagingItems<CharacterModel>,
    onEvent: (CharacterUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PagedContent(
        items = characters,
        emptyMessageRes = R.string.characters_empty,
        modifier = modifier
    ) {
        CharacterList(
            characters = characters,
            onCharacterClick = { onEvent(CharacterUiEvent.CharacterClicked(it)) }
        )
    }
}

@Composable
private fun CharacterList(
    characters: LazyPagingItems<CharacterModel>,
    onCharacterClick: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
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

        pagingAppendFooter(characters)
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

private const val CHARACTER_CONTENT_TYPE = "character"
